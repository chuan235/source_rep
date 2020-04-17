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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.management.JMException;
import org.apache.yetus.audience.InterfaceAudience;
import org.apache.zookeeper.audit.ZKAuditProvider;
import org.apache.zookeeper.jmx.ManagedUtil;
import org.apache.zookeeper.metrics.MetricsProvider;
import org.apache.zookeeper.metrics.MetricsProviderLifeCycleException;
import org.apache.zookeeper.metrics.impl.MetricsProviderBootstrap;
import org.apache.zookeeper.server.admin.AdminServer;
import org.apache.zookeeper.server.admin.AdminServer.AdminServerException;
import org.apache.zookeeper.server.admin.AdminServerFactory;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog.DatadirException;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig.ConfigException;
import org.apache.zookeeper.server.util.JvmPauseMonitor;
import org.apache.zookeeper.util.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class starts and runs a standalone ZooKeeperServer.
 */
@InterfaceAudience.Public
public class ZooKeeperServerMain {

    private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperServerMain.class);

    private static final String USAGE = "Usage: ZooKeeperServerMain configfile | port datadir [ticktime] [maxcnxns]";

    // ZooKeeper server supports two kinds of connection: unencrypted and encrypted.
    private ServerCnxnFactory cnxnFactory;
    private ServerCnxnFactory secureCnxnFactory;
    private ContainerManager containerManager;
    private MetricsProvider metricsProvider;
    private AdminServer adminServer;

    /**
     * 单机模式的启动：QuorumPeerMain#main => ZooKeeperServerMain#main
     * Start up the ZooKeeper server.
     * @param args the configfile or the port datadir [ticktime]
     */
    public static void main(String[] args) {
        ZooKeeperServerMain main = new ZooKeeperServerMain();
        try {
            // 单机的初始化和启动
            main.initializeAndRun(args);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid arguments, exiting abnormally", e);
            LOG.info(USAGE);
            System.err.println(USAGE);
            ZKAuditProvider.addServerStartFailureAuditLog();
            ServiceUtils.requestSystemExit(ExitCode.INVALID_INVOCATION.getValue());
        } catch (ConfigException e) {
            LOG.error("Invalid config, exiting abnormally", e);
            System.err.println("Invalid config, exiting abnormally");
            ZKAuditProvider.addServerStartFailureAuditLog();
            ServiceUtils.requestSystemExit(ExitCode.INVALID_INVOCATION.getValue());
        } catch (DatadirException e) {
            LOG.error("Unable to access datadir, exiting abnormally", e);
            System.err.println("Unable to access datadir, exiting abnormally");
            ZKAuditProvider.addServerStartFailureAuditLog();
            ServiceUtils.requestSystemExit(ExitCode.UNABLE_TO_ACCESS_DATADIR.getValue());
        } catch (AdminServerException e) {
            LOG.error("Unable to start AdminServer, exiting abnormally", e);
            System.err.println("Unable to start AdminServer, exiting abnormally");
            ZKAuditProvider.addServerStartFailureAuditLog();
            ServiceUtils.requestSystemExit(ExitCode.ERROR_STARTING_ADMIN_SERVER.getValue());
        } catch (Exception e) {
            LOG.error("Unexpected exception, exiting abnormally", e);
            ZKAuditProvider.addServerStartFailureAuditLog();
            ServiceUtils.requestSystemExit(ExitCode.UNEXPECTED_ERROR.getValue());
        }
        LOG.info("Exiting normally");
        ServiceUtils.requestSystemExit(ExitCode.EXECUTION_FINISHED.getValue());
    }

    protected void initializeAndRun(String[] args) throws ConfigException, IOException, AdminServerException {
        try {
            ManagedUtil.registerLog4jMBeans();
        } catch (JMException e) {
            LOG.warn("Unable to register log4j JMX control", e);
        }
        // 解析配置文件
        ServerConfig config = new ServerConfig();
        if (args.length == 1) {
            config.parse(args[0]);
        } else {
            config.parse(args);
        }
        // 运行单机模式
        runFromConfig(config);
    }

    /**
     * Run from a ServerConfig.
     * @param config ServerConfig to use.
     * @throws IOException
     * @throws AdminServerException
     */
    public void runFromConfig(ServerConfig config) throws IOException, AdminServerException {
        LOG.info("Starting server 开始启动单机的zookeeper");
        FileTxnSnapLog txnLog = null;
        try {
            try {
                metricsProvider = MetricsProviderBootstrap.startMetricsProvider(
                    config.getMetricsProviderClassName(),
                    config.getMetricsProviderConfiguration());
            } catch (MetricsProviderLifeCycleException error) {
                throw new IOException("Cannot boot MetricsProvider " + config.getMetricsProviderClassName(), error);
            }
            ServerMetrics.metricsProviderInitialized(metricsProvider);
            // Note that this thread isn't going to be doing anything else,
            // so rather than spawning another thread, we will just call run() in this thread.
            // create a file logger url from the command line args
            // 日志工具
            // zk的数据：
            //  log4j的日志数据(zk运行日志)  ->  日志文件由log4j.properties配置
            //  zk的事务日志 -> dataLogDir  zoo.cfg
            //  zk的快照数据 -> dataDir     zoo.cfg
            txnLog = new FileTxnSnapLog(config.dataLogDir, config.dataDir);
            JvmPauseMonitor jvmPauseMonitor = null;
            if (config.jvmPauseMonitorToRun) {
                // jvm暂停的监控器
                jvmPauseMonitor = new JvmPauseMonitor(config);
            }
            // 创建了一个zkServer implements Provider
            final ZooKeeperServer zkServer = new ZooKeeperServer(jvmPauseMonitor, txnLog, config.tickTime, config.minSessionTimeout, config.maxSessionTimeout, config.listenBacklog, null, config.initialConfig);
            txnLog.setServerStats(zkServer.serverStats());

            // Registers shutdown handler which will be used to know the server error or shutdown state changes.
            // 服务器的错误或者关机时会触发这个
            final CountDownLatch shutdownLatch = new CountDownLatch(1);
            // 注册服务挂了的回调方法，挂了之后这里的主线程会立马停止等待，执行关闭服务的逻辑
            zkServer.registerServerShutdownHandler(new ZooKeeperServerShutdownHandler(shutdownLatch));

            // Start Admin server  DummyAdminServer JettyAdminServer 默认是DummyAdminServer
            adminServer = AdminServerFactory.createAdminServer();
            adminServer.setZooKeeperServer(zkServer);
            adminServer.start();

            boolean needStartZKServer = true;
            if (config.getClientPortAddress() != null) {
                // 启动cnxnFactory 监听客户端端口的线程
                cnxnFactory = ServerCnxnFactory.createFactory();
                cnxnFactory.configure(config.getClientPortAddress(), config.getMaxClientCnxns(), config.getClientPortListenBacklog(), false);
                /**
                 *启动方法
                 *  NIOServerCnxnFactory#startup
                 * 1.启动work线程池
                 * 2.初始化ZKDatabase
                 * 3.启动sessionTracker
                 * 4.初始化请求处理链
                 * 5.唤醒ZookeeperServer
                 */
                cnxnFactory.startup(zkServer);
                // zkServer has been started. So we don't need to start it again in secureCnxnFactory.
                needStartZKServer = false;
            }
            if (config.getSecureClientPortAddress() != null) {
                // SSL的客户端端口监听线程  secureClientPortAddress
                secureCnxnFactory = ServerCnxnFactory.createFactory();
                // 初始化接受请求的线程  检测连接的线程
                secureCnxnFactory.configure(config.getSecureClientPortAddress(), config.getMaxClientCnxns(), config.getClientPortListenBacklog(), true);
                secureCnxnFactory.startup(zkServer, needStartZKServer);
            }
            // 启动检查定时器
            containerManager = new ContainerManager(
                zkServer.getZKDatabase(),
                zkServer.firstProcessor,
                Integer.getInteger("znode.container.checkIntervalMs", (int) TimeUnit.MINUTES.toMillis(1)),
                Integer.getInteger("znode.container.maxPerMinute", 10000),
                Long.getLong("znode.container.maxNeverUsedIntervalMs", 0)
            );
            containerManager.start();
            ZKAuditProvider.addZKStartStopAuditLog();

            // Watch status of ZooKeeper server. It will do a graceful shutdown if the server is not running or hits an internal error.
            // 阻塞当前线程  监听器countDownLatch
            shutdownLatch.await();
            // 关闭zk
            shutdown();
            if (cnxnFactory != null) {
                /**
                 * 这里调用了join方法，这个方法的意思就是等待cnxnFactory执行完这里的线程才会继续执行
                 * 下面的secureCnxnFactory和这里同理
                 */
                cnxnFactory.join();
            }
            if (secureCnxnFactory != null) {
                secureCnxnFactory.join();
            }
            if (zkServer.canShutdown()) {
                zkServer.shutdown(true);
            }
        } catch (InterruptedException e) {
            // warn, but generally this is ok
            LOG.warn("Server interrupted", e);
        } finally {
            if (txnLog != null) {
                txnLog.close();
            }
            if (metricsProvider != null) {
                try {
                    metricsProvider.stop();
                } catch (Throwable error) {
                    LOG.warn("Error while stopping metrics", error);
                }
            }
        }
    }

    /**
     * Shutdown the serving instance
     */
    protected void shutdown() {
        if (containerManager != null) {
            containerManager.stop();
        }
        if (cnxnFactory != null) {
            cnxnFactory.shutdown();
        }
        if (secureCnxnFactory != null) {
            secureCnxnFactory.shutdown();
        }
        try {
            if (adminServer != null) {
                adminServer.shutdown();
            }
        } catch (AdminServerException e) {
            LOG.warn("Problem stopping AdminServer", e);
        }
    }

    // VisibleForTesting
    ServerCnxnFactory getCnxnFactory() {
        return cnxnFactory;
    }

}
