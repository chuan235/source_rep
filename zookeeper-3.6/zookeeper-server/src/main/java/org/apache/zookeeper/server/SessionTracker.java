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

package org.apache.zookeeper.server;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.SessionExpiredException;

/**
 * This is the basic interface that ZooKeeperServer uses to track sessions.
 * The standalone and leader ZooKeeperServer use the same SessionTracker.
 * The FollowerZooKeeperServer uses a SessionTracker which is basically a simple shell to track information to be forwarded to the leader.
 * 单机模式和集群模式使用的是同一个SessionTracker
 * Zookeeper服务端管理session的接口
 * follower中的sessionTracker主要同步Leader的session信息和转发给Leader信息
 */
public interface SessionTracker {

    /**
     * zookeeper中session的定义
     */
    interface Session {

        long getSessionId();
        int getTimeout();
        boolean isClosing();

    }

    /**
     * session过期的定义
     */
    interface SessionExpirer {
        void expire(Session session);
        long getServerId();

    }

    /**
     * 创建session
     * @param sessionTimeout session过期时间
     * @return
     */
    long createSession(int sessionTimeout);

    /**
     * Track the session expire, not add to ZkDb.
     * 跟踪session是否过期，不会存储到ZK Database
     * @param id sessionId
     * @param to sessionTimeout
     * @return whether the session was newly tracked (if false, already tracked) true表示没有过期  false表示过期
     */
    boolean trackSession(long id, int to);

    /**
     * Add the session to the local session map or global one in zkDB.
     * 添加session到本地的Map缓存或者一个全局ZK Database缓存
     * @param id sessionId
     * @param to sessionTimeout
     * @return whether the session was newly added (if false, already existed)
     * @return 表示这个session是否添加成功  false表示已存在相同的sssion添加失败
     */
    boolean commitSession(long id, int to);

    /**
     * 表示session是否存活 false表示session已死
     * @param sessionId
     * @param sessionTimeout
     * @return false if session is no longer active
     */
    boolean touchSession(long sessionId, int sessionTimeout);

    /**
     * Mark that the session is in the process of closing.
     * 给session设置一个关闭标记
     * @param sessionId
     */
    void setSessionClosing(long sessionId);

    /**
     * 关闭sessionTracker服务
     */
    void shutdown();

    /**
     * 从缓存中移除session
     * @param sessionId
     */
    void removeSession(long sessionId);

    /**
     * session是否存在SessionTracker中  SessionTracker认不认识这个session
     * @param sessionId
     * @return whether or not the SessionTracker is aware of this session
     */
    boolean isTrackingSession(long sessionId);

    /**
     * Checks whether the SessionTracker is aware of this session, the session is still active, and the owner matches.
     * If the owner wasn't previously set, this sets the owner of the session.
     * UnknownSessionException should never been thrown to the client. It is only used internally to deal with possible local session from other machine
     * 检测SessionTracker能否识别session
     *   1、SessionTracker中存在这个session
     *   2、session状态是存活
     *   3、session与拥有者匹配
     * 如果session没有设置拥有者，那么在这里设置拥有者
     *
     * @param sessionId
     * @param owner
     */
    void checkSession(long sessionId, Object owner) throws KeeperException.SessionExpiredException, KeeperException.SessionMovedException, KeeperException.UnknownSessionException;

    /**
     * Strictly check that a given session is a global session or not
     * 检查session是不是全局session
     * @param sessionId
     * @param owner
     * @throws KeeperException.SessionExpiredException
     * @throws KeeperException.SessionMovedException
     */
    void checkGlobalSession(long sessionId, Object owner) throws KeeperException.SessionExpiredException, KeeperException.SessionMovedException;

    /**
     * 设置session的拥有者
     * @param id
     * @param owner
     * @throws SessionExpiredException
     */
    void setOwner(long id, Object owner) throws SessionExpiredException;

    /**
     * Text dump of session information, suitable for debugging.
     * 使用文本的形式转储session 信息，适合调试
     * @param pwriter the output writer
     */
    void dumpSessions(PrintWriter pwriter);

    /**
     * Returns a mapping of time to session IDs that expire at that time.
     * 返回session与session到期时间的映射Map
     */
    Map<Long, Set<Long>> getSessionExpiryMap();

    /**
     * 如果SessionTracker支持local session，这里会返回具体支持数量  不支持则返回0
     * If this session tracker supports local sessions, return how many. otherwise returns 0;
     */
    long getLocalSessionCount();

    /**
     * SessionTracker是否支持local session
     * @return
     */
    boolean isLocalSessionsEnabled();
}
