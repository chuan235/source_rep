package top.gmfcj.client.zookeeper;

import org.apache.log4j.PropertyConfigurator;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.List;

/**
 * @description: zookeeper的原生客户端测试
 * 存在的问题：
 *  1、客户端在连接zk时，会设置一个sessionTimeout，并且客户端会给服务器发送心跳以及刷新服务端的session时间
 *  当网络断开之后，服务无法接收到客户端的心跳信息，会进行sessionTimeout的倒计时
 *  一旦超过这个时间，就发送session过期，就算后来网络通了，客户端连上了服务器，就是接收session过期的事件，从而删除临时节点和watcher
 * 原生客户端不会重建sesssion
 * 2、watcher只生效一次
 */
public class ZookeeperClientTest {

    public static void preLog(){
        PropertyConfigurator.configure("D:\\workspace\\source_rep\\zookeeper-3.6\\conf\\log4j.properties");
    }

    public static void main(String[] args) throws Exception {
        preLog();
        ZooKeeper client = new ZooKeeper("localhost:2181", 600000, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                System.out.println("zookeeper 启动了");
            }
        });

        // /LOCK
//        String path = client.create("/LOCK/test", new byte[2], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
//        List<String> children = client.getChildren("/LOCK", false);
//
//        System.out.println(children);
//
//
//        client.close();
        //ACL acl = new ACL(ZooDefs.Perms.ALL,ZooDefs.Ids.AUTH_IDS);
        // 创建一个临时节点
//        client.create("/pro", "zookeeperClient".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
//        System.out.println("创建节点完成");
        Stat stat = new Stat();
        byte[] data = client.getData("/pro", new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                if(event.getType().equals(Watcher.Event.EventType.NodeDataChanged)){
                    // 如果其他客户端修改了这个数据，那么这里会监听到，但是只会生效一次
                    // 如果这里生效了，那么整个watcher就相当于执行完了
                    System.out.println("/pro数据改变了");
                }else if(event.getType().equals(Event.EventType.NodeCreated)){
                    System.out.println("创建了子节点");
                }
            }
        }, stat);
//        System.out.println("pro节点数据:"+new String(data));
        // 修改数据
//        client.setData("/pro/temp", "tempArr".getBytes(), 0);
//
//        client.getData("/pro",false, new AsyncCallback.DataCallback(){
//
//            @Override
//            public void processResult(int rc, String path, Object ctx, byte[] data, Stat stat) {
//
//            }
//        }, stat);
        // 阻塞线程
        System.in.read();
    }

}
