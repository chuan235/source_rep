package top.gmfcj.client.curator;

import org.apache.curator.CuratorZookeeperClient;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import sun.print.BackgroundLookupListener;
import top.gmfcj.client.zookeeper.ZookeeperClientTest;

import java.util.List;

/**
 * Curator模块介绍
 *  Recipes:内部包含了zookeeper的典型应用场景，其实现都是基于Curator Framework
 *  Framework：Zookeeper API的封装，简化了Zk客户端编程，添加了连接管理、重试机制等
 *  Utilities：为Zookeeper提供的各种实用程序
 *  Client：Zookeeper Client的封装，用于取代原生的zookeeper客户端
 *  Error：Curator处理错误的封装，连接问题、可恢复的例外等
 * 优点：
 * 1、封装了zookeeper client与zookeeper server之间的连接处理
 * 2、提供了一套Fluent/函数式编程风格的API
 * 3、提供Zookeeper各种应用场景（recipe 共享锁 集群选举方案）
 * 连接超时时间 默认：15000
 */
public class CuratorTest {

    public static final String ZK_CLIENT = "192.168.222.129:2181";
    public static final int SESSION_OUTTIME = 15000;

    public static void main(String[] args) throws Exception {
        ZookeeperClientTest.preLog();
        // 重试策略：初试时间为1s 重试10次
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
        // 构建客户端
        // CuratorFrameworkFactory.newClient()
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(ZK_CLIENT)
                .sessionTimeoutMs(SESSION_OUTTIME)
                .retryPolicy(retryPolicy)
                // 指定命名空间
                .namespace("super")
                .build();
        // 开启连接
        client.start();
//        List<String> list = client.getChildren().forPath("/super");
//        for (String child : list) {
//            System.out.println(child);
//        }
//        Stat stat = client.checkExists().forPath("/super/c3");
        System.out.println(new String(client.getData().forPath("/c2")));
        System.in.read();
    }


    public static void step1(CuratorFramework client) throws Exception {
        // 增加删除节点
        client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath("/super/c1","c1内容".getBytes());
        // 读取节点
        System.out.println("创建节点完成，节点内容"+new String(client.getData().forPath("/super/c1")));
        // 修改节点
        client.setData().forPath("/super/c1", "c1内容已经修改".getBytes());
        System.out.println("修改节点完成，节点新内容"+new String(client.getData().forPath("/super/c1")));
        client.delete().deletingChildrenIfNeeded().forPath("/super/c1");
        System.out.println("节点已删除");
        // 绑定回调函数
        client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT)
                // callback callback+executor
                .inBackground(
                        (CuratorFramework curatorFramework, CuratorEvent curatorEvent) -> {
                            System.out.println("code:" + curatorEvent.getResultCode());
                            System.out.println("type:" + curatorEvent.getType());
                        }
                )
                .forPath("/super/c2","c2内容".getBytes());
    }
}
