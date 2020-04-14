package top.gmfcj.client.zkclient;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.SerializableSerializer;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import sun.plugin2.message.EventMessage;
import top.gmfcj.client.zookeeper.ZookeeperClientTest;

import java.io.IOException;
import java.util.Map;

/**
 * @description: apache zkClient
 * @Link api查询：https://blog.csdn.net/yuan1164345228/article/details/86320218
 */
public class ZkClientTest {

    public static void main(String[] args) throws IOException {
        ZookeeperClientTest.preLog();
        ZkClient client = new ZkClient("192.168.222.129:2181", 300000);
        // 设置序列化机制
        // org.I0Itec.zkclient.exception.ZkMarshallingError: java.io.StreamCorruptedExc
        client.setZkSerializer(new MyZkSerializer());
        // 创建节点
        client.create("/zkclient", "helloZkclient", CreateMode.PERSISTENT);
        // deleteRecursive自动删除子节点
        // client.deleteRecursive("/temp");
//        client.createEphemeral("/tmep12", "tempnode");
//        client.createPersistent("/temp", "tempnode");
        // 读取节点
        System.out.println(client.readData("/zkclient",true).toString());
        // 事件监听机制
        client.subscribeDataChanges("/zkclient", new TempZkListener());
        // 更新数据
        // void writeData(final String path,Object data,final int expectedVersion)
        System.in.read();

    }
    static class TempZkListener implements IZkDataListener{
        @Override
        public void handleDataChange(String s, Object o) throws Exception {
            System.out.println("节点="+s);
            System.out.println("数据="+o.toString());
            System.out.println("数据变化了...");
        }

        @Override
        public void handleDataDeleted(String s) throws Exception {
            System.out.println(s+"节点被删除了...");
        }
    }
}
