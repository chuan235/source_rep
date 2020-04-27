package top.gmfcj.zkmt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2020-04-21 13:15
 */
public class NIOClient {

    public static Selector selector;
    public static SocketChannel channel;

    public static void main(String[] args) throws IOException {
        selector = Selector.open();
        // zk
        channel = SocketChannel.open();
        channel.configureBlocking(false);

        SelectionKey selectionKey = channel.register(selector, SelectionKey.OP_CONNECT);
        boolean flag = channel.connect(new InetSocketAddress("localhost", 10083));
        System.out.println("finishConnect = "+channel.finishConnect());
        if (flag) {
            System.out.println("连接成功注册读事件 flag=" + flag);
            System.out.println("primeConnection .......");
        } else {
            System.out.println("连接失败");
        }

        channel.register(selector, SelectionKey.OP_READ);

        new Thread( () -> {
            while (true) {//一直循环
                try {
                    selector.select();//多路复用器开始监听
                    //获取已经注册在多了复用器上的key通道集
                    Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                    //遍历
                    while (keys.hasNext()) {
                        SelectionKey key = keys.next();//获取key
                        //如果是有效的
                        if (key.isValid()) {
                            // 如果为可读状态,读取服务端返回的数据
                            if (key.isReadable()) {
                                System.out.println("read....");
                            }
                        }
                        //从容器中移除处理过的key
                        keys.remove();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}

