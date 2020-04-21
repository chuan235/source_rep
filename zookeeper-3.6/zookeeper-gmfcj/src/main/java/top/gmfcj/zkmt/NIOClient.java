package top.gmfcj.zkmt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
    public static SocketChannel clntChan;

    static {
        try {
            // 先open  然后connect
            selector = Selector.open();
            clntChan = SocketChannel.open();
            clntChan.configureBlocking(false);
            clntChan.connect(new InetSocketAddress("localhost", 10083));
            clntChan.register(selector, SelectionKey.OP_READ);
            while (!clntChan.finishConnect()){

            }
            System.out.println("已连接！");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public static void main(String[] args) throws IOException {
        SocketChannel socketChannel = NIOClient.clntChan;
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        new Avca(selector,socketChannel).start();
        while (true){
            Scanner scanner = new Scanner(System.in);
            String word = scanner.nextLine();
            byteBuffer.put(word.getBytes());
            byteBuffer.flip();
            socketChannel.write(byteBuffer);
            byteBuffer.clear();
        }
    }
}

class Avca extends Thread{
    private Selector selector;
    private SocketChannel clntChan;

    public Avca(Selector selector,SocketChannel clntChan){
        this.selector = selector;
        this.clntChan = clntChan;
    }

    @Override
    public void run(){
        try {
            while (true){
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = keys.iterator();
                ByteBuffer byteBuffer = ByteBuffer.allocate(256);
                while (keyIterator.hasNext()){
                    SelectionKey selectionKey = keyIterator.next();
                    if (selectionKey.isValid()){
                        if (selectionKey.isReadable()){
                            SocketChannel socketChannel = (SocketChannel)selectionKey.channel();
                            socketChannel.read(byteBuffer);
                            byteBuffer.flip();
                            byte[] bytes = new byte[byteBuffer.remaining()];
                            byteBuffer.get(bytes);
                            System.out.println(new String(bytes));
                            byteBuffer.clear();
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
