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

package org.apache.zookeeper.server.quorum.flexible;

import java.util.Map;
import java.util.Set;

import org.apache.zookeeper.server.quorum.QuorumPeer.QuorumServer;

/**
 * All quorum validators have to implement a method called
 * containsQuorum, which verifies if a HashSet of server
 * identifiers constitutes a quorum.
 */

public interface QuorumVerifier {
    /**
     * 服务器的权重
     *
     * @param id serverId
     * @return
     */
    long getWeight(long id);

    /**
     * set集合中的serverId 是否满足 过半机制的验证
     *
     * @param set
     * @return
     */
    boolean containsQuorum(Set<Long> set);

    /**
     * 配置文件中的version  默认是0
     *
     * @return
     */
    long getVersion();

    void setVersion(long ver);

    /**
     * 返回所有的服务器 <serverId, QuorumServer>
     *
     * @return
     */
    Map<Long, QuorumServer> getAllMembers();

    /**
     * 返回所有可以投票的服务器 <serverId, QuorumServer>
     *
     * @return
     */
    Map<Long, QuorumServer> getVotingMembers();

    /**
     * 返回所有的Observer <serverId, QuorumServer>
     *
     * @return
     */
    Map<Long, QuorumServer> getObservingMembers();

    boolean equals(Object o);

    String toString();

}
