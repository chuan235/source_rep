/**
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
package org.apache.zookeeper;

import org.apache.yetus.audience.InterfaceAudience;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.KeeperException;

/***
 *  CreateMode value determines how the znode is created on ZooKeeper.
 */
@InterfaceAudience.Public
public enum CreateMode {

    /**
     * The znode will not be automatically deleted upon client's disconnect.
     * 持久化节点：创建后就一直存在，直到有删除操作来删除它
     */
    PERSISTENT (0, false, false),
    /**
     * The znode will not be automatically deleted upon client's disconnect,
     * and its name will be appended with a monotonically increasing number.
     * 持久化的顺序节点,基本特性和持久化节点基本一致
     * 特点就是：在zk中，每个父节点会为它的第一级子节点维护一份时序，会记录每一个子节点创建的先后顺序。
     * 基于这个特点，顺序节点就会在创建的过程中会自动给每一个节点后面加上一个数字后缀，作为新节点的名称。数字后缀的范围是0-整数的最大值
     */
    PERSISTENT_SEQUENTIAL (2, false, true),
    /**
     * The znode will be deleted upon the client's disconnect.
     * 临时节点，临时节点的生命周期和客户端会话绑定
     * 也就是说，如果客户端会话失效，那么这个节点就会被自动清除掉。
     * 这里的会话失效不等于会话中断，如果是会话中断重新连接后依然可以在一定时间内看到这些临时节点
     * 过了一段时间后，这个界节点就消失了，因为session已经超时了，临时节点没了
     */
    EPHEMERAL (1, true, false),
    /**
     * The znode will be deleted upon the client's disconnect, and its name
     * will be appended with a monotonically increasing number.
     * 顺序临时节点
     */
    EPHEMERAL_SEQUENTIAL (3, true, true);

    private static final Logger LOG = LoggerFactory.getLogger(CreateMode.class);

    private boolean ephemeral;
    private boolean sequential;
    private int flag;

    CreateMode(int flag, boolean ephemeral, boolean sequential) {
        this.flag = flag;
        this.ephemeral = ephemeral;
        this.sequential = sequential;
    }

    public boolean isEphemeral() {
        return ephemeral;
    }

    public boolean isSequential() {
        return sequential;
    }

    public int toFlag() {
        return flag;
    }

    /**
     * Map an integer value to a CreateMode value
     */
    static public CreateMode fromFlag(int flag) throws KeeperException {
        switch(flag) {
            case 0: return CreateMode.PERSISTENT;

            case 1: return CreateMode.EPHEMERAL;

            case 2: return CreateMode.PERSISTENT_SEQUENTIAL;

            case 3: return CreateMode.EPHEMERAL_SEQUENTIAL ;

            default:
                String errMsg = "Received an invalid flag value: " + flag
                        + " to convert to a CreateMode";
                LOG.error(errMsg);
                throw new KeeperException.BadArgumentsException(errMsg);
        }
    }
}
