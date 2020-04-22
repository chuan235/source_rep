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

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.zookeeper.common.Time;
import org.apache.zookeeper.jmx.MBeanRegistry;
import org.apache.zookeeper.server.ZooKeeperThread;
import org.apache.zookeeper.server.quorum.QuorumCnxManager.Message;
import org.apache.zookeeper.server.quorum.QuorumPeer.LearnerType;
import org.apache.zookeeper.server.quorum.QuorumPeer.ServerState;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.apache.zookeeper.server.quorum.flexible.QuorumVerifier;
import org.apache.zookeeper.server.util.ZxidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of leader election using TCP. It uses an object of the class
 * QuorumCnxManager to manage connections. Otherwise, the algorithm is push-based
 * as with the other UDP implementations.
 * <p>
 * There are a few parameters that can be tuned to change its behavior. First,
 * finalizeWait determines the amount of time to wait until deciding upon a leader.
 * This is part of the leader election algorithm.
 *
 * 使用TCP实现领导者选举，使用QuorumCnxManager来管理连接，除此之外，其他和UDP一样，基于数据的推送
 * 有一些参数可以调整行为。 finalizeWait确定在确定领导者之前要等待的时间
 */

public class FastLeaderElection implements Election {

    private static final Logger LOG = LoggerFactory.getLogger(FastLeaderElection.class);

    /**
     * Determine how much time a process has to wait
     * once it believes that it has reached the end of
     * leader election.
     */
    static final int finalizeWait = 200;

    /**
     * Upper bound on the amount of time between two consecutive
     * notification checks. This impacts the amount of time to get
     * the system up again after long partitions. Currently 60 seconds.
     */

    private static int maxNotificationInterval = 60000;

    /**
     * Lower bound for notification check. The observer don't need to use
     * the same lower bound as participant members
     */
    private static int minNotificationInterval = finalizeWait;

    /**
     * Minimum notification interval, default is equal to finalizeWait
     */
    public static final String MIN_NOTIFICATION_INTERVAL = "zookeeper.fastleader.minNotificationInterval";

    /**
     * Maximum notification interval, default is 60s
     */
    public static final String MAX_NOTIFICATION_INTERVAL = "zookeeper.fastleader.maxNotificationInterval";

    static {
        minNotificationInterval = Integer.getInteger(MIN_NOTIFICATION_INTERVAL, minNotificationInterval);
        LOG.info("{}={}", MIN_NOTIFICATION_INTERVAL, minNotificationInterval);
        maxNotificationInterval = Integer.getInteger(MAX_NOTIFICATION_INTERVAL, maxNotificationInterval);
        LOG.info("{}={}", MAX_NOTIFICATION_INTERVAL, maxNotificationInterval);
    }

    /**
     * Connection manager. Fast leader election uses TCP for
     * communication between peers, and QuorumCnxManager manages
     * such connections.
     */

    QuorumCnxManager manager;

    private SyncedLearnerTracker leadingVoteSet;

    /**
     * Notifications are messages that let other peers know that
     * a given peer has changed its vote, either because it has
     * joined leader election or because it learned of another
     * peer with higher zxid or same zxid and higher server id
     */

    public static class Notification {
        /*
         * Format version, introduced in 3.4.6
         */

        public static final int CURRENTVERSION = 0x2;
        int version;

        /*
         * Proposed leader
         */
        long leader;

        /*
         * zxid of the proposed leader
         */
        long zxid;

        /*
         * Epoch
         */
        long electionEpoch;

        /*
         * current state of sender
         */
        QuorumPeer.ServerState state;

        /*
         * Address of sender
         */
        long sid;

        QuorumVerifier qv;
        /*
         * epoch of the proposed leader
         */
        long peerEpoch;

    }

    static byte[] dummyData = new byte[0];

    /**
     * Messages that a peer wants to send to other peers.
     * These messages can be both Notifications and Acks
     * of reception of notification.
     */
    public static class ToSend {

        enum mType {
            crequest,
            challenge,
            notification,
            ack
        }

        ToSend(mType type, long leader, long zxid, long electionEpoch, ServerState state, long sid, long peerEpoch, byte[] configData) {

            this.leader = leader;
            this.zxid = zxid;
            this.electionEpoch = electionEpoch;
            this.state = state;
            this.sid = sid;
            this.peerEpoch = peerEpoch;
            this.configData = configData;
        }

        /*
         * Proposed leader in the case of notification
         */
        long leader;

        /*
         * id contains the tag for acks, and zxid for notifications
         */
        long zxid;

        /*
         * Epoch
         */
        long electionEpoch;

        /*
         * Current state;
         */
        QuorumPeer.ServerState state;

        /*
         * Address of recipient
         */
        long sid;

        /*
         * Used to send a QuorumVerifier (configuration info)
         */
        byte[] configData = dummyData;

        /*
         * Leader epoch
         */
        long peerEpoch;

    }

    /**
     * 选票发送队列，保存待发送的选票
     */
    LinkedBlockingQueue<ToSend> sendqueue;
    /**
     * 选票接收队列，保存接收到的选票
     */
    LinkedBlockingQueue<Notification> recvqueue;

    /**
     * Multi-threaded implementation of message handler. Messenger
     * implements two sub-classes: WorkReceiver and  WorkSender. The
     * functionality of each is obvious from the name. Each of these
     * spawns a new thread.
     */

    protected class Messenger {

        /**
         * Receives messages from instance of QuorumCnxManager on
         * method run(), and processes such messages.
         * 接收选票信息的线程
         */
        class WorkerReceiver extends ZooKeeperThread {

            volatile boolean stop;
            QuorumCnxManager manager;

            WorkerReceiver(QuorumCnxManager manager) {
                super("WorkerReceiver");
                this.stop = false;
                this.manager = manager;
            }

            public void run() {

                Message response;
                while (!stop) {
                    // Sleeps on receive
                    try {
                        // 从 QuorumCnxManager.recvQueue 队列中取数据  3秒钟之内拿不到就返回
                        response = manager.pollRecvQueue(3000, TimeUnit.MILLISECONDS);
                        if (response == null) {
                            continue;
                        }
                        // 判断取出message的长度
                        final int capacity = response.buffer.capacity();
                        // The current protocol and two previous generations all send at least 28 bytes
                        // 无论是哪一种协议，选票的长度至少是28字节
                        if (capacity < 28) {
                            LOG.error("Got a short response from server {}: {}", response.sid, capacity);
                            continue;
                        }

                        // this is the backwardCompatibility mode in place before ZK-107
                        // It is for a version of the protocol in which we didn't send peer epoch
                        // With peer epoch and version the message became 40 bytes
                        // 对比epoch 和 version 的消息长度是40
                        boolean backCompatibility28 = (capacity == 28);

                        // this is the backwardCompatibility mode for no version information
                        // 没有版本信息的向后兼容
                        boolean backCompatibility40 = (capacity == 40);

                        response.buffer.clear();

                        // Instantiate Notification and set its attributes
                        Notification n = new Notification();
                        // 机器状态
                        int rstate = response.buffer.getInt();
                        // leader
                        long rleader = response.buffer.getLong();
                        // 最后一次的事务id
                        long rzxid = response.buffer.getLong();
                        // 选择的epoch
                        long relectionEpoch = response.buffer.getLong();
                        long rpeerepoch;

                        int version = 0x0;
                        QuorumVerifier rqv = null;

                        try {
                            if (!backCompatibility28) {
                                rpeerepoch = response.buffer.getLong();
                                if (!backCompatibility40) {
                                    // Version added in 3.4.6
                                    version = response.buffer.getInt();
                                } else {
                                    LOG.info("Backward compatibility mode (36 bits), server id: {}", response.sid);
                                }
                            } else {
                                LOG.info("Backward compatibility mode (28 bits), server id: {}", response.sid);
                                rpeerepoch = ZxidUtils.getEpochFromZxid(rzxid);
                            }

                            // check if we have a version that includes config. If so extract config info from message.
                            if (version > 0x1) {
                                int configLength = response.buffer.getInt();

                                // we want to avoid errors caused by the allocation of a byte array with negative length
                                // (causing NegativeArraySizeException) or huge length (causing e.g. OutOfMemoryError)
                                if (configLength < 0 || configLength > capacity) {
                                    throw new IOException(String.format("Invalid configLength in notification message! sid=%d, capacity=%d, version=%d, configLength=%d",
                                            response.sid, capacity, version, configLength));
                                }

                                byte[] b = new byte[configLength];
                                response.buffer.get(b);

                                synchronized (self) {
                                    try {
                                        rqv = self.configFromString(new String(b));
                                        QuorumVerifier curQV = self.getQuorumVerifier();
                                        if (rqv.getVersion() > curQV.getVersion()) {
                                            LOG.info("{} Received version: {} my version: {}",
                                                    self.getId(),
                                                    Long.toHexString(rqv.getVersion()),
                                                    Long.toHexString(self.getQuorumVerifier().getVersion()));
                                            if (self.getPeerState() == ServerState.LOOKING) {
                                                LOG.debug("Invoking processReconfig(), state: {}", self.getServerState());
                                                self.processReconfig(rqv, null, null, false);
                                                if (!rqv.equals(curQV)) {
                                                    LOG.info("restarting leader election");
                                                    self.shuttingDownLE = true;
                                                    self.getElectionAlg().shutdown();

                                                    break;
                                                }
                                            } else {
                                                LOG.debug("Skip processReconfig(), state: {}", self.getServerState());
                                            }
                                        }
                                    } catch (IOException | ConfigException e) {
                                        LOG.error("Something went wrong while processing config received from {}", response.sid);
                                    }
                                }
                            } else {
                                LOG.info("Backward compatibility mode (before reconfig), server id: {}", response.sid);
                            }
                        } catch (BufferUnderflowException | IOException e) {
                            LOG.warn("Skipping the processing of a partial / malformed response message sent by sid={} (message length: {})",
                                    response.sid, capacity, e);
                            continue;
                        }
                        /*
                         * If it is from a non-voting server (such as an observer or a non-voting follower), respond right away.
                         * 如果选票是来自observer的，这张选票就是无效的
                         */
                        if (!validVoter(response.sid)) {
                            // 这里就表示这张票无效
                            Vote current = self.getCurrentVote();
                            QuorumVerifier qv = self.getQuorumVerifier();
                            ToSend notmsg = new ToSend(
                                    ToSend.mType.notification,
                                    current.getId(),
                                    current.getZxid(),
                                    logicalclock.get(),
                                    self.getPeerState(),
                                    response.sid,
                                    current.getPeerEpoch(),
                                    qv.toString().getBytes());
                            sendqueue.offer(notmsg);
                        } else {
                            // Receive new message
                            LOG.debug("Receive new notification message. My id = {}", self.getId());

                            // State of peer that sent this message
                            QuorumPeer.ServerState ackstate = QuorumPeer.ServerState.LOOKING;
                            switch (rstate) {
                                case 0:
                                    ackstate = QuorumPeer.ServerState.LOOKING;
                                    break;
                                case 1:
                                    ackstate = QuorumPeer.ServerState.FOLLOWING;
                                    break;
                                case 2:
                                    ackstate = QuorumPeer.ServerState.LEADING;
                                    break;
                                case 3:
                                    ackstate = QuorumPeer.ServerState.OBSERVING;
                                    break;
                                default:
                                    continue;
                            }
                            // 组装接收到的选票信息
                            n.leader = rleader;
                            n.zxid = rzxid;
                            n.electionEpoch = relectionEpoch;
                            n.state = ackstate;
                            n.sid = response.sid;
                            n.peerEpoch = rpeerepoch;
                            n.version = version;
                            n.qv = rqv;
                            /*
                             * Print notification info
                             */
                            LOG.info(
                                    "Notification: my state:{}; n.sid:{}, n.state:{}, n.leader:{}, n.round:0x{}, "
                                            + "n.peerEpoch:0x{}, n.zxid:0x{}, message format version:0x{}, n.config version:0x{}",
                                    self.getPeerState(),
                                    n.sid,
                                    n.state,
                                    n.leader,
                                    Long.toHexString(n.electionEpoch),
                                    Long.toHexString(n.peerEpoch),
                                    Long.toHexString(n.zxid),
                                    Long.toHexString(n.version),
                                    (n.qv != null ? (Long.toHexString(n.qv.getVersion())) : "0"));

                            /*
                             * If this server is looking, then send proposed leader
                             * 如果自己当前是Looking的，说明正在进行领导者选举
                             */
                            if (self.getPeerState() == QuorumPeer.ServerState.LOOKING) {
                                // 接收到的选票信息加入到 recvqueue，由 WorkerReceiver 线程处理
                                recvqueue.offer(n);
                                /*
                                 * Send a notification back if the peer that sent this
                                 * message is also looking and its logical clock is lagging behind.
                                 * 如果发送选票的server也处于Looking状态，并且发送过来的选票轮次小于当前的选票轮次
                                 * 说明发送过来的选票已经落后了，则将自己最新的选票发送给server
                                 */
                                if ((ackstate == QuorumPeer.ServerState.LOOKING)
                                        && (n.electionEpoch < logicalclock.get())) {
                                    Vote v = getVote();
                                    QuorumVerifier qv = self.getQuorumVerifier();
                                    ToSend notmsg = new ToSend(
                                            ToSend.mType.notification,
                                            v.getId(),
                                            v.getZxid(),
                                            logicalclock.get(),
                                            self.getPeerState(),
                                            response.sid,
                                            v.getPeerEpoch(),
                                            qv.toString().getBytes());
                                    sendqueue.offer(notmsg);
                                }
                            } else {
                                /*
                                 * If this server is not looking, but the one that sent the ack
                                 * is looking, then send back what it believes to be the leader.
                                 */
                                // 如果自己现在不是Looking状态，那么有可能自己是Leader，或者自己是Follower
                                // 如果这时接受到了其他机器的投票，这里就会把当前自己认为是Leader的机器投票信息发送给其他机器
                                Vote current = self.getCurrentVote();
                                // ackstate是接受的投票中的服务器的状态
                                if (ackstate == QuorumPeer.ServerState.LOOKING) {
                                    if (self.leader != null) {
                                        if (leadingVoteSet != null) {
                                            self.leader.setLeadingVoteSet(leadingVoteSet);
                                            leadingVoteSet = null;
                                        }
                                        self.leader.reportLookingSid(response.sid);
                                    }


                                    LOG.debug(
                                            "Sending new notification. My id ={} recipient={} zxid=0x{} leader={} config version = {}",
                                            self.getId(),
                                            response.sid,
                                            Long.toHexString(current.getZxid()),
                                            current.getId(),
                                            Long.toHexString(self.getQuorumVerifier().getVersion()));

                                    QuorumVerifier qv = self.getQuorumVerifier();
                                    ToSend notmsg = new ToSend(
                                            ToSend.mType.notification,
                                            current.getId(),
                                            current.getZxid(),
                                            current.getElectionEpoch(),
                                            self.getPeerState(),
                                            response.sid,
                                            current.getPeerEpoch(),
                                            qv.toString().getBytes());
                                    // 就把当前机器上的leader信息加入到发送数据的队列中，后续就会把这个信息发送给新加入的机器
                                    // 新加入的机器获取到了这个信息之后会在领导者选举的逻辑中进行判断，这里这个新加入的机器不止会收到一个这样的信息
                                    // 而是会收到整个集群对他发送的信息，那么他在选举的判断中，明显会超出一半的机器选了同一个leader
                                    // 这个机器就会主动的去连接Leader，就和leader和follower的交互一致了，建立连接、同步数据、请求转发、ack、commit...
                                    sendqueue.offer(notmsg);
                                }
                            }
                        }
                    } catch (InterruptedException e) {
                        LOG.warn("Interrupted Exception while waiting for new message", e);
                    }
                }
                LOG.info("WorkerReceiver is down");
            }

        }

        /**
         * This worker simply dequeues a message to send and queues it on the manager's queue.
         * 发送消息的线程
         */
        class WorkerSender extends ZooKeeperThread {

            volatile boolean stop;
            QuorumCnxManager manager;

            WorkerSender(QuorumCnxManager manager) {
                super("WorkerSender");
                this.stop = false;
                this.manager = manager;
            }

            public void run() {
                while (!stop) {
                    try {
                        // 从sendqueue取出数据发送出去,会给每一个节点都发送一份
                        ToSend m = sendqueue.poll(3000, TimeUnit.MILLISECONDS);
                        if (m == null) {
                            continue;
                        }

                        process(m);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                LOG.info("WorkerSender is down");
            }

            /**
             * Called by run() once there is a new message to send.
             *
             * @param m message to send
             */
            void process(ToSend m) {
                ByteBuffer requestBuffer = buildMsg(m.state.ordinal(), m.leader, m.zxid, m.electionEpoch, m.peerEpoch, m.configData);
                // 发送数据
                manager.toSend(m.sid, requestBuffer);

            }

        }

        WorkerSender ws;
        WorkerReceiver wr;
        Thread wsThread = null;
        Thread wrThread = null;

        /**
         * Constructor of class Messenger.
         *
         * @param manager Connection manager
         */
        Messenger(QuorumCnxManager manager) {
            // 构建 WorkerSender 和 WorkerReceiver
            this.ws = new WorkerSender(manager);
            this.wr = new WorkerReceiver(manager);

            // 启动发送消息和接收消息的线程
            this.wsThread = new Thread(this.ws, "WorkerSender[myid=" + self.getId() + "]");
            this.wsThread.setDaemon(true);

            this.wrThread = new Thread(this.wr, "WorkerReceiver[myid=" + self.getId() + "]");
            this.wrThread.setDaemon(true);
        }

        /**
         * Starts instances of WorkerSender and WorkerReceiver
         */
        void start() {
            this.wsThread.start();
            this.wrThread.start();
        }

        /**
         * Stops instances of WorkerSender and WorkerReceiver
         */
        void halt() {
            this.ws.stop = true;
            this.wr.stop = true;
        }

    }

    QuorumPeer self;
    Messenger messenger;
    /**
     * Election instance
     */
    AtomicLong logicalclock = new AtomicLong();
    long proposedLeader;
    long proposedZxid;
    long proposedEpoch;

    /**
     * Returns the current vlue of the logical clock counter
     */
    public long getLogicalClock() {
        return logicalclock.get();
    }

    static ByteBuffer buildMsg(int state, long leader, long zxid, long electionEpoch, long epoch) {
        byte[] requestBytes = new byte[40];
        ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);

        /*
         * Building notification packet to send, this is called directly only in tests
         */

        requestBuffer.clear();
        requestBuffer.putInt(state);
        requestBuffer.putLong(leader);
        requestBuffer.putLong(zxid);
        requestBuffer.putLong(electionEpoch);
        requestBuffer.putLong(epoch);
        requestBuffer.putInt(0x1);

        return requestBuffer;
    }

    static ByteBuffer buildMsg(int state, long leader, long zxid, long electionEpoch, long epoch, byte[] configData) {
        byte[] requestBytes = new byte[44 + configData.length];
        ByteBuffer requestBuffer = ByteBuffer.wrap(requestBytes);

        /*
         * Building notification packet to send
         */

        requestBuffer.clear();
        requestBuffer.putInt(state);
        requestBuffer.putLong(leader);
        requestBuffer.putLong(zxid);
        requestBuffer.putLong(electionEpoch);
        requestBuffer.putLong(epoch);
        requestBuffer.putInt(Notification.CURRENTVERSION);
        requestBuffer.putInt(configData.length);
        requestBuffer.put(configData);

        return requestBuffer;
    }

    /**
     * Constructor of FastLeaderElection. It takes two parameters, one
     * is the QuorumPeer object that instantiated this object, and the other
     * is the connection manager. Such an object should be created only once
     * by each peer during an instance of the ZooKeeper service.
     *
     * @param self    QuorumPeer that created this object
     * @param manager Connection manager
     */
    public FastLeaderElection(QuorumPeer self, QuorumCnxManager manager) {
        this.stop = false;
        this.manager = manager;
        starter(self, manager);
    }

    /**
     * This method is invoked by the constructor. Because it is a
     * part of the starting procedure of the object that must be on
     * any constructor of this class, it is probably best to keep as
     * a separate method. As we have a single constructor currently,
     * it is not strictly necessary to have it separate.
     *
     * @param self    QuorumPeer that created this object
     * @param manager Connection manager
     */
    private void starter(QuorumPeer self, QuorumCnxManager manager) {
        this.self = self;
        proposedLeader = -1;
        proposedZxid = -1;

        sendqueue = new LinkedBlockingQueue<ToSend>();
        recvqueue = new LinkedBlockingQueue<Notification>();
        this.messenger = new Messenger(manager);
    }

    /**
     * This method starts the sender and receiver threads.
     * QuorumPeer#createElectionAlgorithm
     */
    public void start() {
        this.messenger.start();
    }

    private void leaveInstance(Vote v) {
        LOG.debug(
                "About to leave FLE instance: leader={}, zxid=0x{}, my id={}, my state={}",
                v.getId(),
                Long.toHexString(v.getZxid()),
                self.getId(),
                self.getPeerState());
        recvqueue.clear();
    }

    public QuorumCnxManager getCnxManager() {
        return manager;
    }

    volatile boolean stop;

    public void shutdown() {
        stop = true;
        proposedLeader = -1;
        proposedZxid = -1;
        leadingVoteSet = null;
        LOG.debug("Shutting down connection manager");
        manager.halt();
        LOG.debug("Shutting down messenger");
        messenger.halt();
        LOG.debug("FLE is down");
    }

    /**
     * Send notifications to all peers upon a change in our vote
     * 给所有参与投票的服务器发送 Notifications
     */
    private void sendNotifications() {
        // 遍历所有的follower
        // 将自己的票发送给这些follower，发送给自己的票是直接添加到接受的队列中的
        for (long sid : self.getCurrentAndNextConfigVoters()) {
            QuorumVerifier qv = self.getQuorumVerifier();
            // 向每一个follower发送 notification
            ToSend notmsg = new ToSend(
                    ToSend.mType.notification,
                    proposedLeader,
                    proposedZxid,
                    logicalclock.get(),
                    QuorumPeer.ServerState.LOOKING,
                    sid,
                    proposedEpoch,
                    qv.toString().getBytes());

            LOG.debug(
                    "Sending Notification: {} (n.leader), 0x{} (n.zxid), 0x{} (n.round), {} (recipient),"
                            + " {} (myid), 0x{} (n.peerEpoch) ",
                    proposedLeader,
                    Long.toHexString(proposedZxid),
                    Long.toHexString(logicalclock.get()),
                    sid,
                    self.getId(),
                    Long.toHexString(proposedEpoch));
            // 添加到发送队列中
            // @see FastLeaderElection.Messenger.WorkerSender#run
            sendqueue.offer(notmsg);
        }
    }

    /**
     * Check if a pair (server id, zxid) succeeds our current vote.
     * 进行选票的比较，比较那一台服务器比较好
     */
    protected boolean totalOrderPredicate(long newId, long newZxid, long newEpoch, long curId, long curZxid, long curEpoch) {
        LOG.debug(
                "id: {}, proposed id: {}, zxid: 0x{}, proposed zxid: 0x{}",
                newId,
                curId,
                Long.toHexString(newZxid),
                Long.toHexString(curZxid));
        if (self.getQuorumVerifier().getWeight(newId) == 0) {
            return false;
        }
        /*
         * We return true if one of the following three cases hold:
         * 1- New epoch is higher
         * 2- New epoch is thes same as current epoch, but new zxid is higher
         * 3- New epoch is the same as current epoch, new zxid is the same as current zxid, but server id is higher.
         * 这里表示选举轮次一致，则依次比较epoch，zxid，serverId
         * 选择 epoch，zxid，serverId 大的服务器，优先级逐渐下降
         * 选出epoch大的 || 选出zxid大的 || 选出serverId大的
         */
        return ((newEpoch > curEpoch)
                || ((newEpoch == curEpoch)
                && ((newZxid > curZxid)
                || ((newZxid == curZxid)
                && (newId > curId)))));
    }

    /**
     * Given a set of votes, return the SyncedLearnerTracker which is used to
     * determines if have sufficient to declare the end of the election round.
     *
     * @param votes Set of votes
     * @param vote  Identifier of the vote received last
     * @return the SyncedLearnerTracker with vote details
     */
    protected SyncedLearnerTracker getVoteTracker(Map<Long, Vote> votes, Vote vote) {
        SyncedLearnerTracker voteSet = new SyncedLearnerTracker();
        voteSet.addQuorumVerifier(self.getQuorumVerifier());
        if (self.getLastSeenQuorumVerifier() != null
                && self.getLastSeenQuorumVerifier().getVersion() > self.getQuorumVerifier().getVersion()) {
            voteSet.addQuorumVerifier(self.getLastSeenQuorumVerifier());
        }

        /*
         * First make the views consistent. Sometimes peers will have different
         * zxids for a server depending on timing.
         */
        for (Map.Entry<Long, Vote> entry : votes.entrySet()) {
            if (vote.equals(entry.getValue())) {
                voteSet.addAck(entry.getKey());
            }
        }

        return voteSet;
    }

    /**
     * In the case there is a leader elected, and a quorum supporting
     * this leader, we have to check if the leader has voted and acked
     * that it is leading. We need this check to avoid that peers keep
     * electing over and over a peer that has crashed and it is no
     * longer leading.
     *
     * @param votes         set of votes
     * @param leader        leader id
     * @param electionEpoch epoch id
     */
    protected boolean checkLeader(Map<Long, Vote> votes, long leader, long electionEpoch) {

        boolean predicate = true;

        /*
         * If everyone else thinks I'm the leader, I must be the leader.
         * The other two checks are just for the case in which I'm not the
         * leader. If I'm not the leader and I haven't received a message
         * from leader stating that it is leading, then predicate is false.
         */

        if (leader != self.getId()) {
            if (votes.get(leader) == null) {
                predicate = false;
            } else if (votes.get(leader).getState() != ServerState.LEADING) {
                predicate = false;
            }
        } else if (logicalclock.get() != electionEpoch) {
            predicate = false;
        }

        return predicate;
    }

    synchronized void updateProposal(long leader, long zxid, long epoch) {
        LOG.debug(
                "Updating proposal: {} (newleader), 0x{} (newzxid), {} (oldleader), 0x{} (oldzxid)",
                leader,
                Long.toHexString(zxid),
                proposedLeader,
                Long.toHexString(proposedZxid));

        proposedLeader = leader;
        proposedZxid = zxid;
        proposedEpoch = epoch;
    }

    public synchronized Vote getVote() {
        return new Vote(proposedLeader, proposedZxid, proposedEpoch);
    }

    /**
     * A learning state can be either FOLLOWING or OBSERVING.
     * This method simply decides which one depending on the
     * role of the server.
     *
     * @return ServerState
     */
    private ServerState learningState() {
        if (self.getLearnerType() == LearnerType.PARTICIPANT) {
            LOG.debug("I am a participant: {}", self.getId());
            return ServerState.FOLLOWING;
        } else {
            LOG.debug("I am an observer: {}", self.getId());
            return ServerState.OBSERVING;
        }
    }

    /**
     * Returns the initial vote value of server identifier.
     *
     * @return long
     */
    private long getInitId() {
        if (self.getQuorumVerifier().getVotingMembers().containsKey(self.getId())) {
            return self.getId();
        } else {
            return Long.MIN_VALUE;
        }
    }

    /**
     * Returns initial last logged zxid.
     *
     * @return long
     */
    private long getInitLastLoggedZxid() {
        if (self.getLearnerType() == LearnerType.PARTICIPANT) {
            return self.getLastLoggedZxid();
        } else {
            return Long.MIN_VALUE;
        }
    }

    /**
     * Returns the initial vote value of the peer epoch.
     *
     * @return long
     */
    private long getPeerEpoch() {
        if (self.getLearnerType() == LearnerType.PARTICIPANT) {
            try {
                return self.getCurrentEpoch();
            } catch (IOException e) {
                RuntimeException re = new RuntimeException(e.getMessage());
                re.setStackTrace(e.getStackTrace());
                throw re;
            }
        } else {
            return Long.MIN_VALUE;
        }
    }

    /**
     * Update the peer state based on the given proposedLeader. Also update
     * the leadingVoteSet if it becomes the leader.
     */
    private void setPeerState(long proposedLeader, SyncedLearnerTracker voteSet) {
        ServerState ss = (proposedLeader == self.getId()) ? ServerState.LEADING : learningState();
        self.setPeerState(ss);
        if (ss == ServerState.LEADING) {
            leadingVoteSet = voteSet;
        }
    }

    /**
     * Starts a new round of leader election. Whenever our QuorumPeer
     * changes its state to LOOKING, this method is invoked, and it
     * sends notifications to all other peers.
     * 只要服务器的状态是LOOKING，就会进入这里，进行领导者的选举
     */
    public Vote lookForLeader() throws InterruptedException {
        try {
            self.jmxLeaderElectionBean = new LeaderElectionBean();
            MBeanRegistry.getInstance().register(self.jmxLeaderElectionBean, self.jmxLocalPeerBean);
        } catch (Exception e) {
            LOG.warn("Failed to register with JMX", e);
            self.jmxLeaderElectionBean = null;
        }

        self.start_fle = Time.currentElapsedTime();
        try {
            /*
             * The votes from the current leader election are stored in recvset. In other words, a vote v is in recvset
             * if v.electionEpoch == logicalclock. The current participant uses recvset to deduce on whether a majority
             * of participants has voted for it.
             */
            // 接收到的所有选票
            Map<Long, Vote> recvset = new HashMap<Long, Vote>();

            /*
             * The votes from previous leader elections, as well as the votes from the current leader election are
             * stored in outofelection. Note that notifications in a LOOKING state are not stored in outofelection.
             * Only FOLLOWING or LEADING notifications are stored in outofelection. The current participant could use
             * outofelection to learn which participant is the leader if it arrives late (i.e., higher logicalclock than
             * the electionEpoch of the received notifications) in a leader election.
             */
            // 发送出去的选票
            Map<Long, Vote> outofelection = new HashMap<Long, Vote>();

            int notTimeout = minNotificationInterval;

            synchronized (this) {
                // 投票的次数每次加1
                logicalclock.incrementAndGet();
                // 更新 Proposal
                updateProposal(getInitId(), getInitLastLoggedZxid(), getPeerEpoch());
            }

            LOG.info("New election. My id = {}, proposed zxid=0x{}", self.getId(), Long.toHexString(proposedZxid));
            // 发送当前机器的选票，启动的时候都是默认投的自己
            // 会直接将票放入到自己的recvQueue中，还会将自己的票发送给其他可以投票的节点
            // @see QuorumCnxManager#toSend
            sendNotifications();

            //
            SyncedLearnerTracker voteSet;

            /*
             * Loop in which we exchange notifications until we find a leader
             */
            // 没有停止，并且当前服务器处于LOOKING状态
            while ((self.getPeerState() == ServerState.LOOKING) && (!stop)) {
                /*
                 * Remove next notification from queue, times out after 2 times
                 * the termination time
                 */
                // 获取其他服务器的投票，也存在自己的票
                Notification n = recvqueue.poll(notTimeout, TimeUnit.MILLISECONDS);

                /*
                 * Sends more notifications if haven't received enough.Otherwise processes new notification.
                 */
                if (n == null) {
                    // 获取到的选票数据为空，则检查有没有需要发送的数据
                    // 因为之前执行过 sendNotifications，默认会有投自己一票的数据，这里没有拿到数据
                    // 可能数据没有读取到或者是连接还没有初始化好
                    if (manager.haveDelivered()) {
                        // 如果都已经发送完成了，再发送一次
                        sendNotifications();
                    } else {
                        // 没有发送完成，连接所有可以投票的服务器
                        manager.connectAll();
                    }

                    // Exponential backoff
                    int tmpTimeOut = notTimeout * 2;
                    notTimeout = Math.min(tmpTimeOut, maxNotificationInterval);
                    LOG.info("Notification time out: {}", notTimeout);
                } else if (validVoter(n.sid) && validVoter(n.leader)) {
                    /*
                     * Only proceed if the vote comes from a replica in the current or next
                     * voting view for a replica in the current or next voting view.
                     */
                    // 接受到了返回的 Notification ，下面的参数n代表的就是投给自己或者是其他服务器发送过来的选票信息
                    switch (n.state) {
                        case LOOKING:
                            // 如果发送选票的服务器也处于LOOKING状态，也在进行领导者选举
                            if (getInitLastLoggedZxid() == -1) {
                                LOG.debug("Ignoring notification as our zxid is -1");
                                break;
                            }
                            if (n.zxid == -1) {
                                LOG.debug("Ignoring notification from member with -1 zxid {}", n.sid);
                                break;
                            }
                            // If notification > current, replace and send messages out
                            if (n.electionEpoch > logicalclock.get()) {
                                // 如果接收到的投票轮次比自己高，说明自己的投票已经落后了，会修改自己的投票
                                // 设置自己的时钟为选举的时钟
                                logicalclock.set(n.electionEpoch);
                                // 清空自己的选票
                                recvset.clear();
                                // 比较本机和传递过来的服务器信息，比较谁的能力更强
                                // 如果选票对应的服务器能力比当前服务器的能力强，那么就更新自己的选票，选择能力更强的服务器
                                if (totalOrderPredicate(n.leader, n.zxid, n.peerEpoch, getInitId(), getInitLastLoggedZxid(), getPeerEpoch())) {
                                    // 修改自己的选票信息
                                    updateProposal(n.leader, n.zxid, n.peerEpoch);
                                } else {
                                    // 否则还是投给自己，将原来的选票丢弃
                                    updateProposal(getInitId(), getInitLastLoggedZxid(), getPeerEpoch());
                                }
                                // 向所有的 participant 发送更新后的选票
                                sendNotifications();
                            } else if (n.electionEpoch < logicalclock.get()) {
                                // 如果接收到的选票轮次比自己服务器的选票轮次低，直接丢弃这次接受的选票，开始获取下一个选票
                                LOG.debug(
                                        "Notification election epoch is smaller than logicalclock. n.electionEpoch = 0x{}, logicalclock=0x{}",
                                        Long.toHexString(n.electionEpoch),
                                        Long.toHexString(logicalclock.get()));
                                // break switch
                                break;
                            } else if (totalOrderPredicate(n.leader, n.zxid, n.peerEpoch, proposedLeader, proposedZxid, proposedEpoch)) {
                                // 如果接受的选票的轮次和当前机器的轮次一致，就开始比较epoch,zxid,serverId
                                // 如果接受到选票的数据更全，则说明接受的选票对应的服务器能力更强
                                // 修改自己的选票为接受过来的选票
                                updateProposal(n.leader, n.zxid, n.peerEpoch);
                                // 将更新的选票信息发送出去
                                sendNotifications();
                            }

                            LOG.debug(
                                    "Adding vote: from={}, proposed leader={}, proposed zxid=0x{}, proposed election epoch=0x{}",
                                    n.sid,
                                    n.leader,
                                    Long.toHexString(n.zxid),
                                    Long.toHexString(n.electionEpoch));
                            // 保存自己更新的选票信息
                            // don't care about the version if it's in LOOKING state
                            recvset.put(n.sid, new Vote(n.leader, n.zxid, n.electionEpoch, n.peerEpoch));
                            // 统计接收到的选票
                            voteSet = getVoteTracker(recvset,
                                    new Vote(proposedLeader, proposedZxid, logicalclock.get(), proposedEpoch));

                            // 根据接受到的选票以及自己的选票，来判断能不能成为Leader
                            if (voteSet.hasAllQuorums()) {
                                // 如果符合过半机制，本台服务器就认为选出了Leader
                                // Verify if there is any change in the proposed leader
                                // 如果在这时又接受到了新的选票，
                                while ((n = recvqueue.poll(finalizeWait, TimeUnit.MILLISECONDS)) != null) {
                                    if (totalOrderPredicate(n.leader, n.zxid, n.peerEpoch, proposedLeader, proposedZxid, proposedEpoch)) {
                                        // 接受的新的选票更加优秀，将这个选票放入接受选票的队列中，并退出这个while
                                        // 退出这个while就意味着放弃当前选出来的Leader
                                        /**
                                         * 其实就是说，如果在leader选出来之后，突然又来了一个选票
                                         * 如果这个选票比当前选出来的leader更加优秀，则将这个选票放回到recvqueue中
                                         *      并退出这个while循环，放弃当前选出来的leader，重新选择leader
                                         * 如果这个选票没有当前选出来的leader优秀，或者没有选票，则认为当前选出来的leader就可以作为集群的leader
                                         */
                                        recvqueue.put(n);
                                        break;
                                    }
                                }

                                /*
                                 * This predicate is true once we don't read any new
                                 * relevant message from the reception queue
                                 */
                                // 如果没有获取到选票了，那么当前集群的leader就算选出来了
                                if (n == null) {
                                    // 如果选出来的leader是当前的节点，那么设置自己为LEADING，如果是其他服务器设置自己服务器对应的状态
                                    // 根据 LearnerType 来进行设置
                                    setPeerState(proposedLeader, voteSet);
                                    // 将最终的投票返回
                                    Vote endVote = new Vote(proposedLeader, proposedZxid, logicalclock.get(), proposedEpoch);
                                    // 清理接受队列
                                    leaveInstance(endVote);
                                    return endVote;
                                }
                            }
                            break;
                        case OBSERVING:
                            LOG.debug("Notification from observer: {}", n.sid);
                            break;
                        case FOLLOWING:
                        case LEADING:
                            /*
                             * Consider all notifications from the same epoch
                             * together.
                             */
                            if (n.electionEpoch == logicalclock.get()) {
                                recvset.put(n.sid, new Vote(n.leader, n.zxid, n.electionEpoch, n.peerEpoch, n.state));
                                voteSet = getVoteTracker(recvset, new Vote(n.version, n.leader, n.zxid, n.electionEpoch, n.peerEpoch, n.state));
                                if (voteSet.hasAllQuorums() && checkLeader(recvset, n.leader, n.electionEpoch)) {
                                    setPeerState(n.leader, voteSet);
                                    Vote endVote = new Vote(n.leader, n.zxid, n.electionEpoch, n.peerEpoch);
                                    leaveInstance(endVote);
                                    return endVote;
                                }
                            }

                            /*
                             * Before joining an established ensemble, verify that
                             * a majority are following the same leader.
                             *
                             * Note that the outofelection map also stores votes from the current leader election.
                             * See ZOOKEEPER-1732 for more information.
                             */
                            outofelection.put(n.sid, new Vote(n.version, n.leader, n.zxid, n.electionEpoch, n.peerEpoch, n.state));
                            voteSet = getVoteTracker(outofelection, new Vote(n.version, n.leader, n.zxid, n.electionEpoch, n.peerEpoch, n.state));

                            if (voteSet.hasAllQuorums() && checkLeader(outofelection, n.leader, n.electionEpoch)) {
                                synchronized (this) {
                                    logicalclock.set(n.electionEpoch);
                                    setPeerState(n.leader, voteSet);
                                }
                                Vote endVote = new Vote(n.leader, n.zxid, n.electionEpoch, n.peerEpoch);
                                leaveInstance(endVote);
                                return endVote;
                            }
                            break;
                        default:
                            LOG.warn("Notification state unrecoginized: {} (n.state), {}(n.sid)", n.state, n.sid);
                            break;
                    }
                } else {
                    if (!validVoter(n.leader)) {
                        LOG.warn("Ignoring notification for non-cluster member sid {} from sid {}", n.leader, n.sid);
                    }
                    if (!validVoter(n.sid)) {
                        LOG.warn("Ignoring notification for sid {} from non-quorum member sid {}", n.leader, n.sid);
                    }
                }
            }
            return null;
        } finally {
            try {
                if (self.jmxLeaderElectionBean != null) {
                    MBeanRegistry.getInstance().unregister(self.jmxLeaderElectionBean);
                }
            } catch (Exception e) {
                LOG.warn("Failed to unregister with JMX", e);
            }
            self.jmxLeaderElectionBean = null;
            LOG.debug("Number of connection processing threads: {}", manager.getConnectionThreadCount());
        }
    }

    /**
     * Check if a given sid is represented in either the current or the next voting view
     * 给定ServerId是否在可以投票的server范围内
     *
     * @param sid Server identifier
     * @return boolean
     */
    private boolean validVoter(long sid) {
        return self.getCurrentAndNextConfigVoters().contains(sid);
    }

}
