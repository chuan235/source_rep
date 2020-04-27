/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server.quorum;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import javax.security.sasl.SaslException;
import org.apache.jute.BinaryInputArchive;
import org.apache.jute.BinaryOutputArchive;
import org.apache.zookeeper.ZooDefs.OpCode;
import org.apache.zookeeper.server.Request;
import org.apache.zookeeper.server.ServerMetrics;
import org.apache.zookeeper.server.TxnLogProposalIterator;
import org.apache.zookeeper.server.ZKDatabase;
import org.apache.zookeeper.server.ZooKeeperThread;
import org.apache.zookeeper.server.ZooTrace;
import org.apache.zookeeper.server.quorum.Leader.Proposal;
import org.apache.zookeeper.server.quorum.QuorumPeer.LearnerType;
import org.apache.zookeeper.server.quorum.auth.QuorumAuthServer;
import org.apache.zookeeper.server.util.MessageTracker;
import org.apache.zookeeper.server.util.ZxidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There will be an instance of this class created by the Leader for each
 * learner. All communication with a learner is handled by this
 * class.
 */
public class LearnerHandler extends ZooKeeperThread {

    private static final Logger LOG = LoggerFactory.getLogger(LearnerHandler.class);

    protected final Socket sock;

    public Socket getSocket() {
        return sock;
    }

    final LearnerMaster learnerMaster;

    /** Deadline for receiving the next ack. If we are bootstrapping then
     * it's based on the initLimit, if we are done bootstrapping it's based
     * on the syncLimit. Once the deadline is past this learner should
     * be considered no longer "sync'd" with the leader. */
    volatile long tickOfNextAckDeadline;

    /**
     * ZooKeeper server identifier of this learner
     */
    protected long sid = 0;

    long getSid() {
        return sid;
    }

    String getRemoteAddress() {
        return sock == null ? "<null>" : sock.getRemoteSocketAddress().toString();
    }

    protected int version = 0x1;

    int getVersion() {
        return version;
    }

    /**
     * The packets to be sent to the learner
     */
    final LinkedBlockingQueue<QuorumPacket> queuedPackets = new LinkedBlockingQueue<QuorumPacket>();
    private final AtomicLong queuedPacketsSize = new AtomicLong();

    protected final AtomicLong packetsReceived = new AtomicLong();
    protected final AtomicLong packetsSent = new AtomicLong();

    protected final AtomicLong requestsReceived = new AtomicLong();

    protected volatile long lastZxid = -1;

    public synchronized long getLastZxid() {
        return lastZxid;
    }

    protected final Date established = new Date();

    public Date getEstablished() {
        return (Date) established.clone();
    }

    /**
     * Marker packets would be added to quorum packet queue after every
     * markerPacketInterval packets.
     * It is ok if packetCounter overflows.
     */
    private final int markerPacketInterval = 1000;
    private AtomicInteger packetCounter = new AtomicInteger();

    /**
     * This class controls the time that the Leader has been
     * waiting for acknowledgement of a proposal from this Learner.
     * If the time is above syncLimit, the connection will be closed.
     * It keeps track of only one proposal at a time, when the ACK for
     * that proposal arrives, it switches to the last proposal received
     * or clears the value if there is no pending proposal.
     */
    private class SyncLimitCheck {

        private boolean started = false;
        private long currentZxid = 0;
        private long currentTime = 0;
        private long nextZxid = 0;
        private long nextTime = 0;

        public synchronized void start() {
            started = true;
        }

        public synchronized void updateProposal(long zxid, long time) {
            if (!started) {
                return;
            }
            if (currentTime == 0) {
                currentTime = time;
                currentZxid = zxid;
            } else {
                nextTime = time;
                nextZxid = zxid;
            }
        }

        public synchronized void updateAck(long zxid) {
            if (currentZxid == zxid) {
                currentTime = nextTime;
                currentZxid = nextZxid;
                nextTime = 0;
                nextZxid = 0;
            } else if (nextZxid == zxid) {
                LOG.warn(
                    "ACK for 0x{} received before ACK for 0x{}",
                    Long.toHexString(zxid),
                    Long.toHexString(currentZxid));
                nextTime = 0;
                nextZxid = 0;
            }
        }

        public synchronized boolean check(long time) {
            if (currentTime == 0) {
                return true;
            } else {
                long msDelay = (time - currentTime) / 1000000;
                return (msDelay < learnerMaster.syncTimeout());
            }
        }

    }

    private SyncLimitCheck syncLimitCheck = new SyncLimitCheck();

    private static class MarkerQuorumPacket extends QuorumPacket {

        long time;
        MarkerQuorumPacket(long time) {
            this.time = time;
        }

        @Override
        public int hashCode() {
            return Objects.hash(time);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            MarkerQuorumPacket that = (MarkerQuorumPacket) o;
            return time == that.time;
        }

    }

    private BinaryInputArchive ia;

    private BinaryOutputArchive oa;

    private final BufferedInputStream bufferedInput;
    private BufferedOutputStream bufferedOutput;

    protected final MessageTracker messageTracker;

    // for test only
    protected void setOutputArchive(BinaryOutputArchive oa) {
        this.oa = oa;
    }
    protected void setBufferedOutput(BufferedOutputStream bufferedOutput) {
        this.bufferedOutput = bufferedOutput;
    }

    /**
     * Keep track of whether we have started send packets thread
     */
    private volatile boolean sendingThreadStarted = false;

    /**
     * For testing purpose, force learnerMaster to use snapshot to sync with followers
     */
    public static final String FORCE_SNAP_SYNC = "zookeeper.forceSnapshotSync";
    private boolean forceSnapSync = false;

    /**
     * Keep track of whether we need to queue TRUNC or DIFF into packet queue
     * that we are going to blast it to the learner
     */
    private boolean needOpPacket = true;

    /**
     * Last zxid sent to the learner as part of synchronization
     */
    private long leaderLastZxid;

    /**
     * for sync throttling
     */
    private LearnerSyncThrottler syncThrottler = null;

    LearnerHandler(Socket sock, BufferedInputStream bufferedInput, LearnerMaster learnerMaster) throws IOException {
        super("LearnerHandler-" + sock.getRemoteSocketAddress());
        this.sock = sock;
        this.learnerMaster = learnerMaster;
        this.bufferedInput = bufferedInput;

        if (Boolean.getBoolean(FORCE_SNAP_SYNC)) {
            forceSnapSync = true;
            LOG.info("Forcing snapshot sync is enabled");
        }

        try {
            QuorumAuthServer authServer = learnerMaster.getQuorumAuthServer();
            if (authServer != null) {
                authServer.authenticate(sock, new DataInputStream(bufferedInput));
            }
        } catch (IOException e) {
            LOG.error("Server failed to authenticate quorum learner, addr: {}, closing connection", sock.getRemoteSocketAddress(), e);
            try {
                sock.close();
            } catch (IOException ie) {
                LOG.error("Exception while closing socket", ie);
            }
            throw new SaslException("Authentication failure: " + e.getMessage());
        }

        this.messageTracker = new MessageTracker(MessageTracker.BUFFERED_MESSAGE_SIZE);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LearnerHandler ").append(sock);
        sb.append(" tickOfNextAckDeadline:").append(tickOfNextAckDeadline());
        sb.append(" synced?:").append(synced());
        sb.append(" queuedPacketLength:").append(queuedPackets.size());
        return sb.toString();
    }

    /**
     * If this packet is queued, the sender thread will exit
     */
    final QuorumPacket proposalOfDeath = new QuorumPacket();

    private LearnerType learnerType = LearnerType.PARTICIPANT;
    public LearnerType getLearnerType() {
        return learnerType;
    }

    /**
     * This method will use the thread to send packets added to the queuedPackets list
     *
     *
     * @throws InterruptedException
     */
    private void sendPackets() throws InterruptedException {
        long traceMask = ZooTrace.SERVER_PACKET_TRACE_MASK;
        // 死循环
        while (true) {
            try {
                QuorumPacket p;
                // 首先去poll
                p = queuedPackets.poll();
                if (p == null) {
                    bufferedOutput.flush();
                    // poll不到  就take阻塞在这里
                    p = queuedPackets.take();
                }
                // 监控队列的大小  packet的数量
                ServerMetrics.getMetrics().LEARNER_HANDLER_QP_SIZE.add(Long.toString(this.sid), queuedPackets.size());
                // MarkerQuorumPacket 表示这个packet只是用来统计的
                if (p instanceof MarkerQuorumPacket) {
                    MarkerQuorumPacket m = (MarkerQuorumPacket) p;
                    ServerMetrics.getMetrics().LEARNER_HANDLER_QP_TIME.add(Long.toString(this.sid), (System.nanoTime() - m.time) / 1000000L);
                    continue;
                }

                queuedPacketsSize.addAndGet(-packetSize(p));
                if (p == proposalOfDeath) {
                    // Packet of death!
                    break;
                }
                if (p.getType() == Leader.PING) {
                    traceMask = ZooTrace.SERVER_PING_TRACE_MASK;
                }
                if (p.getType() == Leader.PROPOSAL) {
                    // 更新发送提议的时间
                    syncLimitCheck.updateProposal(p.getZxid(), System.nanoTime());
                }
                if (LOG.isTraceEnabled()) {
                    ZooTrace.logQuorumPacket(LOG, traceMask, 'o', p);
                }
                // Log the zxid of the last request, if it is a valid zxid.
                if (p.getZxid() > 0) {
                    lastZxid = p.getZxid();
                }
                // 写入流
                oa.writeRecord(p, "packet");
                // 统计send数
                packetsSent.incrementAndGet();
                messageTracker.trackSent(p.getType());
            } catch (IOException e) {
                if (!sock.isClosed()) {
                    LOG.warn("Unexpected exception at {}", this, e);
                    try {
                        // this will cause everything to shutdown on
                        // this learner handler and will help notify
                        // the learner/observer instantaneously
                        sock.close();
                    } catch (IOException ie) {
                        LOG.warn("Error closing socket for handler {}", this, ie);
                    }
                }
                break;
            }
        }
    }

    public static String packetToString(QuorumPacket p) {
        String type;
        String mess = null;

        switch (p.getType()) {
        case Leader.ACK:
            type = "ACK";
            break;
        case Leader.COMMIT:
            type = "COMMIT";
            break;
        case Leader.FOLLOWERINFO:
            type = "FOLLOWERINFO";
            break;
        case Leader.NEWLEADER:
            type = "NEWLEADER";
            break;
        case Leader.PING:
            type = "PING";
            break;
        case Leader.PROPOSAL:
            type = "PROPOSAL";
            break;
        case Leader.REQUEST:
            type = "REQUEST";
            break;
        case Leader.REVALIDATE:
            type = "REVALIDATE";
            ByteArrayInputStream bis = new ByteArrayInputStream(p.getData());
            DataInputStream dis = new DataInputStream(bis);
            try {
                long id = dis.readLong();
                mess = " sessionid = " + id;
            } catch (IOException e) {
                LOG.warn("Unexpected exception", e);
            }

            break;
        case Leader.UPTODATE:
            type = "UPTODATE";
            break;
        case Leader.DIFF:
            type = "DIFF";
            break;
        case Leader.TRUNC:
            type = "TRUNC";
            break;
        case Leader.SNAP:
            type = "SNAP";
            break;
        case Leader.ACKEPOCH:
            type = "ACKEPOCH";
            break;
        case Leader.SYNC:
            type = "SYNC";
            break;
        case Leader.INFORM:
            type = "INFORM";
            break;
        case Leader.COMMITANDACTIVATE:
            type = "COMMITANDACTIVATE";
            break;
        case Leader.INFORMANDACTIVATE:
            type = "INFORMANDACTIVATE";
            break;
        default:
            type = "UNKNOWN" + p.getType();
        }
        String entry = null;
        if (type != null) {
            entry = type + " " + Long.toHexString(p.getZxid()) + " " + mess;
        }
        return entry;
    }

    /**
     * This thread will receive packets from the peer and process them and
     * also listen to new connections from new peers.
     */
    @Override
    public void run() {
        try {
            learnerMaster.addLearnerHandler(this);
            tickOfNextAckDeadline = learnerMaster.getTickOfInitialAckDeadline();

            ia = BinaryInputArchive.getArchive(bufferedInput);
            bufferedOutput = new BufferedOutputStream(sock.getOutputStream());
            oa = BinaryOutputArchive.getArchive(bufferedOutput);
            QuorumPacket qp = new QuorumPacket();
            /**
             * 接收 follower或者observer发送过来的 注册信息
             * 这里发送过来的 protocolVersion = 0x10000 读入到qp中
             * @see Learner#registerWithLeader
             */
            ia.readRecord(qp, "packet");
            messageTracker.trackReceived(qp.getType());
            if (qp.getType() != Leader.FOLLOWERINFO && qp.getType() != Leader.OBSERVERINFO) {
                LOG.error("First packet {} is not FOLLOWERINFO or OBSERVERINFO!", qp.toString());

                return;
            }

            if (learnerMaster instanceof ObserverMaster && qp.getType() != Leader.OBSERVERINFO) {
                throw new IOException("Non observer attempting to connect to ObserverMaster. type = " + qp.getType());
            }
            byte[] learnerInfoData = qp.getData();
            // 读取客户端的serverId
            if (learnerInfoData != null) {
                ByteBuffer bbsid = ByteBuffer.wrap(learnerInfoData);
                if (learnerInfoData.length >= 8) {
                    this.sid = bbsid.getLong();
                }
                if (learnerInfoData.length >= 12) {
                    this.version = bbsid.getInt(); // protocolVersion
                }
                if (learnerInfoData.length >= 20) {
                    long configVersion = bbsid.getLong();
                    if (configVersion > learnerMaster.getQuorumVerifierVersion()) {
                        throw new IOException("Follower is ahead of the leader (has a later activated configuration)");
                    }
                }
            } else {
                this.sid = learnerMaster.getAndDecrementFollowerCounter();
            }
            // 从leader管理的集群中找serverId
            String followerInfo = learnerMaster.getPeerInfo(this.sid);
            if (followerInfo.isEmpty()) {
                // 找不到，未知的server
                LOG.info(
                    "Follower sid: {} not in the current config {}",
                    this.sid,
                    Long.toHexString(learnerMaster.getQuorumVerifierVersion()));
            } else {
                LOG.info("Follower sid: {} : info : {}", this.sid, followerInfo);
            }
            // 如果客户端是Observer
            if (qp.getType() == Leader.OBSERVERINFO) {
                learnerType = LearnerType.OBSERVER;
            }

            learnerMaster.registerLearnerHandlerBean(this, sock);
            // 获取客户端的zxid计算出对应的 Epoch
            long lastAcceptedEpoch = ZxidUtils.getEpochFromZxid(qp.getZxid());

            long peerLastZxid;
            StateSummary ss = null;
            long zxid = qp.getZxid();
            // 产生新的 Epoch ，已连接中follower和leader中的最大的epoch+1
            long newEpoch = learnerMaster.getEpochToPropose(this.getSid(), lastAcceptedEpoch);
            // 生成新的zxid
            long newLeaderZxid = ZxidUtils.makeZxid(newEpoch, 0);
            // 这个肯定不小于，传递过来的 protocolVersion = 0x10000
            if (this.getVersion() < 0x10000) {
                // we are going to have to extrapolate the epoch information
                long epoch = ZxidUtils.getEpochFromZxid(zxid);
                ss = new StateSummary(epoch, zxid);
                // fake the message
                learnerMaster.waitForEpochAck(this.getSid(), ss);
            } else {
                byte[] ver = new byte[4];
                ByteBuffer.wrap(ver).putInt(0x10000);
                // 构建一个 LEADERINFO 类型的packet 传递 最新的 ZxId 和 Epoch
                QuorumPacket newEpochPacket = new QuorumPacket(Leader.LEADERINFO, newLeaderZxid, ver, null);
                oa.writeRecord(newEpochPacket, "packet");
                messageTracker.trackSent(Leader.LEADERINFO);
                bufferedOutput.flush();
                QuorumPacket ackEpochPacket = new QuorumPacket();
                // 读取follower节点响应的ACK包， 这个ack包中，就包含了同步之前的最后一次zxid epoch
                ia.readRecord(ackEpochPacket, "packet");
                messageTracker.trackReceived(ackEpochPacket.getType());
                if (ackEpochPacket.getType() != Leader.ACKEPOCH) {
                    LOG.error("{} is not ACKEPOCH", ackEpochPacket.toString());
                    return;
                }
                ByteBuffer bbepoch = ByteBuffer.wrap(ackEpochPacket.getData());
                // Zxid： 32的Epoch + 32位递增的proposal 这里的epoch可以是-1
                ss = new StateSummary(bbepoch.getInt(), ackEpochPacket.getZxid());
                // 等待zxid
                learnerMaster.waitForEpochAck(this.getSid(), ss);
            }
            // epoch已经确定，接下来就是同步数据的过程
            // 这里获取learner节点上的最后一次事务zxid  =>  old epoch
            peerLastZxid = ss.getLastZxid();
            /**
             * 在这里实现了先回滚在差异化同步的逻辑吗？  实现了的（这里传递过来的zxid是learner上的上一次epoch生成的事务id）
             * @see LearnerHandler#queueCommittedProposals
            这里就会从快照中找出比peerLastZxid大的所有记录外加一个比peerLastZxid小的记录，并且循坏这些快照记录，首先是集合中的最小的zxid和learner的zxid进行比较
            1、这两个相等，那么就发送diff，下次就不用再来判断了
            2、如果不相等就只有可能是集合中的zxid > learnerZxid ，这就说明learnerZxid对应的记录在leader中不存在，
                这里就要使用trun让learner回滚到 prevProposalZxid 附近，后续直接将prevProposalZxid之后的记录发送给learner
                learner的prevProposalZxid记录会保留
             * 新加入的follower也会在这里同步数据吗？   会的
             *
             */
            // Take any necessary action if we need to send TRUNC or DIFF
            // 如果需要发送TRUNC或DIFF,请采取必须的措施.在任何情况下都将调用 startForwarding
            // startForwarding() will be called in all cases
            boolean needSnap = syncFollower(peerLastZxid, learnerMaster);

            // syncs between followers and the leader are exempt from throttling because it is importatnt to keep the state of quorum servers up-to-date.
            // The exempted syncs are counted as concurrent syncs though  采用并发同步
            // leader 与 follower 之间的同步不受限制。只需保证集群验证器中的数据最新即可

            boolean exemptFromThrottle = getLearnerType() != LearnerType.OBSERVER;

            /** if we are not truncating or sending a diff just send a snapshot */
            if (needSnap) {
                syncThrottler = learnerMaster.getLearnerSnapSyncThrottler();
                // follower -> true
                // observer -> false
                // 统计同步数据的节点数量
                syncThrottler.beginSync(exemptFromThrottle);
                try {
                    long zxidToSend = learnerMaster.getZKDatabase().getDataTreeLastProcessedZxid();
                    // 先发送一个标志  表示要发送快照数据了  这里是直接写入到socket中了
                    oa.writeRecord(new QuorumPacket(Leader.SNAP, zxidToSend, null, null), "packet");
                    messageTracker.trackSent(Leader.SNAP);
                    bufferedOutput.flush();
                    LOG.info(
                        "Sending snapshot last zxid of peer is 0x{}, zxid of leader is 0x{}, "
                            + "send zxid of db as 0x{}, {} concurrent snapshot sync, "
                            + "snapshot sync was {} from throttle",
                        Long.toHexString(peerLastZxid),
                        Long.toHexString(leaderLastZxid),
                        Long.toHexString(zxidToSend),
                        syncThrottler.getSyncInProgress(),
                        exemptFromThrottle ? "exempt" : "not exempt");
                    // Dump data to peer  直接把快照写入socket
                    learnerMaster.getZKDatabase().serializeSnapshot(oa);
                    oa.writeString("BenWasHere", "signature");
                    bufferedOutput.flush();
                } finally {
                    ServerMetrics.getMetrics().SNAP_COUNT.add(1);
                }
            } else {
                syncThrottler = learnerMaster.getLearnerDiffSyncThrottler();
                syncThrottler.beginSync(exemptFromThrottle);
                ServerMetrics.getMetrics().DIFF_COUNT.add(1);
            }

            LOG.debug("Sending NEWLEADER message to {}", sid);
            // the version of this quorumVerifier will be set by leader.lead() in case
            // the leader is just being established. waitForEpochAck makes sure that readyToStart is true if
            // we got here, so the version was set
            if (getVersion() < 0x10000) {
                QuorumPacket newLeaderQP = new QuorumPacket(Leader.NEWLEADER, newLeaderZxid, null, null);
                oa.writeRecord(newLeaderQP, "packet");
            } else {
                // 当leader当前的第一个zxid  发送给learner
                // 表示后续生成的zxid在这个zxid的基础上增加  =>  统一集群的zxid
                QuorumPacket newLeaderQP = new QuorumPacket(Leader.NEWLEADER, newLeaderZxid, learnerMaster.getQuorumVerifierBytes(), null);
                // 把packet放入queuedPackets队列
                // 表示这里会在数据同步之后才会去统一集群的zxid
                queuedPackets.add(newLeaderQP);
            }
            bufferedOutput.flush();

            // Start thread that blast packets in the queue to learner
            // 开启一个线程向learner发送packet
            startSendingPackets();

            // Have to wait for the first ACK, wait until the learnerMaster is ready, and only then we can start processing messages.
            // 接收learner的响应，等待
            qp = new QuorumPacket();
            ia.readRecord(qp, "packet");

            messageTracker.trackReceived(qp.getType());
            if (qp.getType() != Leader.ACK) {
                LOG.error("Next packet was supposed to be an ACK, but received packet: {}", packetToString(qp));
                return;
            }

            LOG.debug("Received NEWLEADER-ACK message from {}", sid);
            // 判断接收到follower的 ACK 是否过半  learnerSid  learnerSid
            learnerMaster.waitForNewLeaderAck(getSid(), qp.getZxid());

            syncLimitCheck.start();
            // sync ends when NEWLEADER-ACK is received
            syncThrottler.endSync();
            syncThrottler = null;

            // now that the ack has been processed expect the syncLimit
            sock.setSoTimeout(learnerMaster.syncTimeout());

            /*
             * Wait until learnerMaster starts up
             */
            learnerMaster.waitForStartup();

            // Mutation packets will be queued during the serialize,
            // so we need to mark when the peer can actually start
            // using the data
            //
            LOG.debug("Sending UPTODATE message to {}", sid);
            queuedPackets.add(new QuorumPacket(Leader.UPTODATE, -1, null, null));

            while (true) {
                qp = new QuorumPacket();
                ia.readRecord(qp, "packet");
                messageTracker.trackReceived(qp.getType());

                long traceMask = ZooTrace.SERVER_PACKET_TRACE_MASK;
                if (qp.getType() == Leader.PING) {
                    traceMask = ZooTrace.SERVER_PING_TRACE_MASK;
                }
                if (LOG.isTraceEnabled()) {
                    ZooTrace.logQuorumPacket(LOG, traceMask, 'i', qp);
                }
                tickOfNextAckDeadline = learnerMaster.getTickOfNextAckDeadline();

                packetsReceived.incrementAndGet();

                ByteBuffer bb;
                long sessionId;
                int cxid;
                int type;

                switch (qp.getType()) {
                case Leader.ACK:
                    if (this.learnerType == LearnerType.OBSERVER) {
                        LOG.debug("Received ACK from Observer {}", this.sid);
                    }
                    syncLimitCheck.updateAck(qp.getZxid());
                    // 处理ACK Leader.processAck
                    learnerMaster.processAck(this.sid, qp.getZxid(), sock.getLocalSocketAddress());
                    break;
                case Leader.PING:
                    // Process the touches
                    ByteArrayInputStream bis = new ByteArrayInputStream(qp.getData());
                    DataInputStream dis = new DataInputStream(bis);
                    while (dis.available() > 0) {
                        long sess = dis.readLong();
                        int to = dis.readInt();
                        learnerMaster.touch(sess, to);
                    }
                    break;
                case Leader.REVALIDATE:
                    ServerMetrics.getMetrics().REVALIDATE_COUNT.add(1);
                    learnerMaster.revalidateSession(qp, this);
                    break;
                case Leader.REQUEST:
                    // 如果是follower发送过来的request
                    bb = ByteBuffer.wrap(qp.getData());
                    sessionId = bb.getLong();
                    cxid = bb.getInt();
                    type = bb.getInt();
                    bb = bb.slice();
                    Request si;
                    // 根据请求类型构建requestPacket
                    if (type == OpCode.sync) {
                        si = new LearnerSyncRequest(this, sessionId, cxid, type, bb, qp.getAuthinfo());
                    } else {
                        si = new Request(null, sessionId, cxid, type, bb, qp.getAuthinfo());
                    }
                    si.setOwner(this);
                    // leader提交请求
                    // prepRequestProcessor.processRequest(request);
                    learnerMaster.submitLearnerRequest(si);
                    requestsReceived.incrementAndGet();
                    break;
                default:
                    LOG.warn("unexpected quorum packet, type: {}", packetToString(qp));
                    break;
                }
            }
        } catch (IOException e) {
            if (sock != null && !sock.isClosed()) {
                LOG.error("Unexpected exception causing shutdown while sock still open", e);
                //close the socket to make sure the
                //other side can see it being close
                try {
                    sock.close();
                } catch (IOException ie) {
                    // do nothing
                }
            }
        } catch (InterruptedException e) {
            LOG.error("Unexpected exception in LearnerHandler.", e);
        } catch (SyncThrottleException e) {
            LOG.error("too many concurrent sync.", e);
            syncThrottler = null;
        } catch (Exception e) {
            LOG.error("Unexpected exception in LearnerHandler.", e);
            throw e;
        } finally {
            if (syncThrottler != null) {
                syncThrottler.endSync();
                syncThrottler = null;
            }
            String remoteAddr = getRemoteAddress();
            LOG.warn("******* GOODBYE {} ********", remoteAddr);
            messageTracker.dumpToLog(remoteAddr);
            shutdown();
        }
    }

    /**
     * Start thread that will forward any packet in the queue to the follower
     */
    protected void startSendingPackets() {
        if (!sendingThreadStarted) {
            // Start sending packets
            new Thread() {
                public void run() {
                    Thread.currentThread().setName("Sender-" + sock.getRemoteSocketAddress());
                    try {
                        sendPackets();
                    } catch (InterruptedException e) {
                        LOG.warn("Unexpected interruption", e);
                    }
                }
            }.start();
            sendingThreadStarted = true;
        } else {
            LOG.error("Attempting to start sending thread after it already started");
        }
    }

    /**
     * Tests need not send marker packets as they are only needed to
     * log quorum packet delays
     */
    protected boolean shouldSendMarkerPacketForLogging() {
        return true;
    }

    /**
     * Determine if we need to sync with follower using DIFF/TRUNC/SNAP
     * and setup follower to receive packets from commit processor
     *
     * @param peerLastZxid  learner 的zxid  => oldEpoch
     * @param learnerMaster leader
     * @return true if snapshot transfer is needed.
     */
    boolean syncFollower(long peerLastZxid, LearnerMaster learnerMaster) {
        /*
         * When leader election is completed, the leader will set its lastProcessedZxid to be (epoch < 32).
         * There will be no txn associated with this zxid.
         *
         * The learner will set its lastProcessedZxid to the same value if it get DIFF or SNAP from the learnerMaster.
         * If the same learner come back to sync with learnerMaster using this zxid,
         * we will never find this zxid in our history.
         * In this case, we will ignore TRUNC logic and always send DIFF if we have old enough history
         */
        boolean isPeerNewEpochZxid = (peerLastZxid & 0xffffffffL) == 0;
        // Keep track of the latest zxid which already queued
        long currentZxid = peerLastZxid;
        boolean needSnap = true;
        ZKDatabase db = learnerMaster.getZKDatabase();
        boolean txnLogSyncEnabled = db.isTxnLogSyncEnabled();
        ReentrantReadWriteLock lock = db.getLogLock();
        ReadLock rl = lock.readLock();
        try {
            rl.lock();
            // 内存中已提交日志记录的最大事务id
            long maxCommittedLog = db.getmaxCommittedLog();
            // 内存中已提交日志记录的最小事务id
            long minCommittedLog = db.getminCommittedLog();
            // dataTree上的最大的zxid
            long lastProcessedZxid = db.getDataTreeLastProcessedZxid();

            LOG.info("Synchronizing with Learner sid: {} maxCommittedLog=0x{}"
                     + " minCommittedLog=0x{} lastProcessedZxid=0x{}"
                     + " peerLastZxid=0x{}",
                     getSid(),
                     Long.toHexString(maxCommittedLog),
                     Long.toHexString(minCommittedLog),
                     Long.toHexString(lastProcessedZxid),
                     Long.toHexString(peerLastZxid));

            if (db.getCommittedLog().isEmpty()) {
                /*
                 * It is possible that committedLog is empty.
                 * In that case setting these value to the latest txn in learnerMaster db
                 * will reduce the case that we need to handle
                 *
                 * Here is how each case handle by the if block below
                 * 1. lastProcessZxid == peerZxid -> Handle by (2)
                 * 2. lastProcessZxid < peerZxid -> Handle by (3)
                 * 3. lastProcessZxid > peerZxid -> Handle by (5)
                 */
                minCommittedLog = lastProcessedZxid;
                maxCommittedLog = lastProcessedZxid;
            }

            /*
             * Here are the cases that we want to handle
             *
             * 1. Force sending snapshot (for testing purpose)
             * 2. Peer and learnerMaster is already sync, send empty diff
             * 3. Follower has txn that we haven't seen. This may be old leader
             *    so we need to send TRUNC. However, if peer has newEpochZxid,
             *    we cannot send TRUNC since the follower has no txnlog
             * 4. Follower is within committedLog range or already in-sync.
             *    We may need to send DIFF or TRUNC depending on follower's zxid.
             *    We always send empty DIFF if follower is already in-sync
             * 5. Follower missed the committedLog. We will try to use on-disk
             *    txnlog + committedLog to sync with follower. If that fail,we will send snapshot
             *
             */

            // peerLastZxid 就是follower或者observer上的最后一次的事务id
            if (forceSnapSync) {
                // 是否进行 强制快照同步
                // Force learnerMaster to use snapshot to sync with follower
                LOG.warn("Forcing snapshot sync - should not see this in production");
            } else if (lastProcessedZxid == peerLastZxid) {
                // Follower is already sync with us, send empty diff
                // 与leader的zxid相同，表示已经同步完成，发送一个空的DIFF
                LOG.info(
                    "Sending DIFF zxid=0x{} for peer sid: {}",
                    Long.toHexString(peerLastZxid),
                    getSid());
                queueOpPacket(Leader.DIFF, peerLastZxid);
                needOpPacket = false;
                needSnap = false;
            } else if (peerLastZxid > maxCommittedLog && !isPeerNewEpochZxid) {
                // Newer than committedLog, send trunc and done
                // 大于了最大的提交提交记录的zxid，说明数据多余了
                LOG.debug(
                    "Sending TRUNC to follower zxidToSend=0x{} for peer sid:{}",
                    Long.toHexString(maxCommittedLog),
                    getSid());
                // 发送TRUNC packet并将leader的最大提交zxid发送过去
                queueOpPacket(Leader.TRUNC, maxCommittedLog);
                currentZxid = maxCommittedLog;
                needOpPacket = false;
                needSnap = false;
            } else if ((maxCommittedLog >= peerLastZxid) && (minCommittedLog <= peerLastZxid)) {
                // Follower is within commitLog range
                // follower的数据在提交范围之内
                LOG.info("Using committedLog for peer sid: {}", getSid());
                Iterator<Proposal> itr = db.getCommittedLog().iterator();
                // 遍历提交zxid记录进行比对，找到需要发送给learner的记录段 并且发送给learner
                /**
                 * itr: 提议记录集合
                 * peerLastZxid： learner的zxid
                 * maxZxid：
                 * maxCommittedLog：最大的已提交zxid
                 * 同步范围就是[peerLastZxid, maxCommittedLog]
                 */
                currentZxid = queueCommittedProposals(itr, peerLastZxid, null, maxCommittedLog);
                needSnap = false;
            } else if (peerLastZxid < minCommittedLog && txnLogSyncEnabled) {
                // Use txnlog and committedLog to sync
                // Calculate sizeLimit that we allow to retrieve txnlog from disk
                // 最近的snapSize * 0.33  可以读取的日志大小  recentSnapt * 0.33
                long sizeLimit = db.calculateTxnLogSizeLimit();
                // This method can return empty iterator if the requested zxid is older than on-disk txnlog
                // 这里就会从快照中找出比peerLastZxid大的所有记录 外加一个比peerLastZxid小的快照记录
                // 如果没有找到或者找到的位置距离最近的snapt大小超过了 sizeLimit 就会返回一个空的Iterator
                Iterator<Proposal> txnLogItr = db.getProposalsFromTxnLog(peerLastZxid, sizeLimit);
                /**
                 * @see TxnLogProposalIterator#hasNext
                 */
                if (txnLogItr.hasNext()) {
                    LOG.info("Use txnlog and committedLog for peer sid: {}", getSid());
                    /**
                     * itr: 事务日志记录集合
                     * peerLastZxid： learner的zxid
                     * minCommittedLog：最小的提交记录
                     * maxCommittedLog：最大的已提交zxid
                     * 同步范围就是[peerLastZxid, maxCommittedLog]
                     */
                    currentZxid = queueCommittedProposals(txnLogItr, peerLastZxid, minCommittedLog, maxCommittedLog);
                    if (currentZxid < minCommittedLog) {
                        LOG.info(
                            "Detected gap between end of txnlog: 0x{} and start of committedLog: 0x{}",
                            Long.toHexString(currentZxid),
                            Long.toHexString(minCommittedLog));
                        currentZxid = peerLastZxid;
                        // Clear out currently queued requests and revert
                        // to sending a snapshot.
                        queuedPackets.clear();
                        needOpPacket = true;
                    } else {
                        LOG.debug("Queueing committedLog 0x{}", Long.toHexString(currentZxid));
                        Iterator<Proposal> committedLogItr = db.getCommittedLog().iterator();
                        currentZxid = queueCommittedProposals(committedLogItr, currentZxid, null, maxCommittedLog);
                        needSnap = false;
                    }
                }
                // closing the resources
                if (txnLogItr instanceof TxnLogProposalIterator) {
                    TxnLogProposalIterator txnProposalItr = (TxnLogProposalIterator) txnLogItr;
                    txnProposalItr.close();
                }
            } else {
                LOG.warn(
                    "Unhandled scenario for peer sid: {} maxCommittedLog=0x{}"
                        + " minCommittedLog=0x{} lastProcessedZxid=0x{}"
                        + " peerLastZxid=0x{} txnLogSyncEnabled={}",
                    getSid(),
                    Long.toHexString(maxCommittedLog),
                    Long.toHexString(minCommittedLog),
                    Long.toHexString(lastProcessedZxid),
                    Long.toHexString(peerLastZxid),
                    txnLogSyncEnabled);
            }
            if (needSnap) {
                currentZxid = db.getDataTreeLastProcessedZxid();
            }

            LOG.debug("Start forwarding 0x{} for peer sid: {}", Long.toHexString(currentZxid), getSid());
            leaderLastZxid = learnerMaster.startForwarding(this, currentZxid);
        } finally {
            rl.unlock();
        }

        if (needOpPacket && !needSnap) {
            // This should never happen, but we should fall back to sending
            // snapshot just in case.
            LOG.error("Unhandled scenario for peer sid: {} fall back to use snapshot",  getSid());
            needSnap = true;
        }

        return needSnap;
    }

    /**
     * Queue committed proposals into packet queue. The range of packets which
     * is going to be queued are (peerLaxtZxid, maxZxid]
     *
     * @param itr  iterator point to the proposals 需要同步的提议集合
     * @param peerLastZxid  last zxid seen by the follower learner的最大zxid
     * @param maxZxid  max zxid of the proposal to queue, null if no limit 提议集合的最大zxid（提议集合的限制）
     * @param lastCommittedZxid when sending diff, we need to send lastCommittedZxid on the leader to follow Zab 1.0 protocol.
     *
     * @return last zxid of the queued proposal
     */
    protected long queueCommittedProposals(Iterator<Proposal> itr, long peerLastZxid, Long maxZxid, Long lastCommittedZxid) {
        // 这里取出来的值其实就是计数器是否是0
        boolean isPeerNewEpochZxid = (peerLastZxid & 0xffffffffL) == 0;
        long queuedZxid = peerLastZxid;
        // as we look through proposals, this variable keeps track of previous
        // proposal Id.
        long prevProposalZxid = -1;
        while (itr.hasNext()) {
            Proposal propose = itr.next();
            // 当前提议的zxid
            long packetZxid = propose.packet.getZxid();
            // abort if we hit the limit  packetZxid超出了同步集合的限制
            if ((maxZxid != null) && (packetZxid > maxZxid)) {
                break;
            }
            // skip the proposals the peer already has
            // packetZxid小于learner的zxid，当前的zxid赋值给prevProposalZxid，后续回滚数据会用到这个值
            if (packetZxid < peerLastZxid) {
                prevProposalZxid = packetZxid;
                continue;
            }
            // If we are sending the first packet, figure out whether to trunc or diff
            // 第一次比较packet，看是否需要对learner上的zxid做出什么操作，比较的zxid一般都是提议中最小的zxid
            if (needOpPacket) {
                // Send diff when we see the follower's zxid in our history
                // 如果第一次比较就相等，直接把lastCommittedZxid发送给follower
                if (packetZxid == peerLastZxid) {
                    LOG.info(
                        "Sending DIFF zxid=0x{}  for peer sid: {}",
                        Long.toHexString(lastCommittedZxid),
                        getSid());
                    // queuedPackets队列中添加一个 DIFF packet  并直接开启下一次循环
                    queueOpPacket(Leader.DIFF, lastCommittedZxid);
                    // 下次就不进入这里了
                    // 1、在commitlog的循环中，如果找到了一个相等的zxid，那么之后的所有zxid可以直接将packet方法放入队列，所以不用再来这个if
                    // 2、在snaplog的循环中，如果找到了一个相等的zxid，那么必定是第一个packet，这之后的也不用进if了
                    needOpPacket = false;
                    continue;
                }
                // 第一次比较不相等，不相等就只有可能是大于，大于则说明当前learner的zxid在leader上不存在
                if (isPeerNewEpochZxid) {
                    // Send diff and fall through if zxid is of a new-epoch
                    LOG.info(
                        "Sending DIFF zxid=0x{}  for peer sid: {}",
                        Long.toHexString(lastCommittedZxid),
                        getSid());
                    queueOpPacket(Leader.DIFF, lastCommittedZxid);
                    needOpPacket = false;
                } else if (packetZxid > peerLastZxid) {
                    // Peer have some proposals that the learnerMaster hasn't seen yet it may used to be a leader
                    // learner节点可能存在提议，在它曾经是leader的时候没有发送成功，只是在本地做了
                    // 对于这种节点的处理，leader会发送最接近peerLastZxid的prevProposalZxid给他，让learner回滚到这个prevProposalZxid附近
                    if (ZxidUtils.getEpochFromZxid(packetZxid) != ZxidUtils.getEpochFromZxid(peerLastZxid)) {
                        // We cannot send TRUNC that cross epoch boundary.
                        // The learner will crash if it is asked to do so.
                        // We will send snapshot this those cases.
                        LOG.warn("Cannot send TRUNC to peer sid: " + getSid() + " peer zxid is from different epoch");
                        return queuedZxid;
                    }
                    // 上一届的epoch相等，首先要求learner将数据恢复到leader中某个日志存在的zxid附近，然后在使用diff的模式，将后续的数据发送过去
                    LOG.info(
                        "Sending TRUNC zxid=0x{}  for peer sid: {}",
                        Long.toHexString(prevProposalZxid),
                        getSid());
                    queueOpPacket(Leader.TRUNC, prevProposalZxid);
                    needOpPacket = false;
                }
            }

            if (packetZxid <= queuedZxid) {
                // 快照记录中的zxid 与 learner的zxid相等 直接跳过
                // We can get here, if we don't have op packet to queue
                // or there is a duplicate txn in a given iterator
                continue;
            }

            // Since this is already a committed proposal, we need to follow it by a commit packet
            queuePacket(propose.packet);
            // 将上面的packetZxid提交
            // 走到这里表示当前的提议是可以接受被接受的，因此会在提议的packet后追加一个commitPacket
            queueOpPacket(Leader.COMMIT, packetZxid);
            // 这个queuedZxid会最终被返回，所以返回的就是最后一次接收提议的pakcet的zxid
            queuedZxid = packetZxid;
        }

        if (needOpPacket && isPeerNewEpochZxid) {
            // We will send DIFF for this kind of zxid in any case. This if-block
            // is the catch when our history older than learner and there is
            // no new txn since then. So we need an empty diff
            LOG.info(
                "Sending TRUNC zxid=0x{}  for peer sid: {}",
                Long.toHexString(lastCommittedZxid),
                getSid());
            queueOpPacket(Leader.DIFF, lastCommittedZxid);
            needOpPacket = false;
        }

        return queuedZxid;
    }

    public void shutdown() {
        // Send the packet of death
        try {
            queuedPackets.clear();
            queuedPackets.put(proposalOfDeath);
        } catch (InterruptedException e) {
            LOG.warn("Ignoring unexpected exception", e);
        }
        try {
            if (sock != null && !sock.isClosed()) {
                sock.close();
            }
        } catch (IOException e) {
            LOG.warn("Ignoring unexpected exception during socket close", e);
        }
        this.interrupt();
        learnerMaster.removeLearnerHandler(this);
        learnerMaster.unregisterLearnerHandlerBean(this);
    }

    public long tickOfNextAckDeadline() {
        return tickOfNextAckDeadline;
    }

    /**
     * ping calls from the learnerMaster to the peers
     */
    public void ping() {
        // If learner hasn't sync properly yet, don't send ping packet
        // otherwise, the learner will crash
        if (!sendingThreadStarted) {
            return;
        }
        long id;
        if (syncLimitCheck.check(System.nanoTime())) {
            id = learnerMaster.getLastProposed();
            QuorumPacket ping = new QuorumPacket(Leader.PING, id, null, null);
            queuePacket(ping);
        } else {
            LOG.warn("Closing connection to peer due to transaction timeout.");
            shutdown();
        }
    }

    /**
     * Queue leader packet of a given type
     * @param type
     * @param zxid
     */
    private void queueOpPacket(int type, long zxid) {
        QuorumPacket packet = new QuorumPacket(type, zxid, null, null);
        queuePacket(packet);
    }

    void queuePacket(QuorumPacket p) {
        queuedPackets.add(p);
        // Add a MarkerQuorumPacket at regular intervals.
        if (shouldSendMarkerPacketForLogging() && packetCounter.getAndIncrement() % markerPacketInterval == 0) {
            queuedPackets.add(new MarkerQuorumPacket(System.nanoTime()));
        }
        queuedPacketsSize.addAndGet(packetSize(p));
    }

    static long packetSize(QuorumPacket p) {
        /* Approximate base size of QuorumPacket: int + long + byte[] + List */
        long size = 4 + 8 + 8 + 8;
        byte[] data = p.getData();
        if (data != null) {
            size += data.length;
        }
        return size;
    }

    public boolean synced() {
        return isAlive() && learnerMaster.getCurrentTick() <= tickOfNextAckDeadline;
    }

    public synchronized Map<String, Object> getLearnerHandlerInfo() {
        Map<String, Object> info = new LinkedHashMap<>(9);
        info.put("remote_socket_address", getRemoteAddress());
        info.put("sid", getSid());
        info.put("established", getEstablished());
        info.put("queued_packets", queuedPackets.size());
        info.put("queued_packets_size", queuedPacketsSize.get());
        info.put("packets_received", packetsReceived.longValue());
        info.put("packets_sent", packetsSent.longValue());
        info.put("requests", requestsReceived.longValue());
        info.put("last_zxid", getLastZxid());

        return info;
    }

    public synchronized void resetObserverConnectionStats() {
        packetsReceived.set(0);
        packetsSent.set(0);
        requestsReceived.set(0);

        lastZxid = -1;
    }

    /**
     * For testing, return packet queue
     * @return
     */
    public Queue<QuorumPacket> getQueuedPackets() {
        return queuedPackets;
    }

    /**
     * For testing, we need to reset this value
     */
    public void setFirstPacket(boolean value) {
        needOpPacket = value;
    }

}
