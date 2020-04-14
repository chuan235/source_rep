package top.gmfcj.client.wait;

import top.gmfcj.client.utils.ThreadUtil;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2019-10-17 11:58
 */
public class WaitCnxn {


    public static LinkedBlockingQueue<WPacket> queue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws Exception{
        WPacket packet = new WPacket();

        queue.add(packet);
        Thread.sleep(300);

        SendThread sendThread = new SendThread();
        new Thread(sendThread,"sendThread").start();


        synchronized (packet){
            while(!packet.finished){
                System.out.println("packet wait .....");
                packet.wait();
            }
            System.out.println("packet response success");
        }
    }

    static class SendThread implements Runnable{
        @Override
        public void run() {
            while(true){
                ThreadUtil.sleep(3000);
                WPacket packet = null;
                try {
                    packet = queue.take();
                    synchronized (packet){
                        packet.setFinished(true);
//                        packet.notifyAll();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("已完成...");
            }
        }
    }

    static class WPacket{
        boolean finished;

        public boolean isFinished() {
            return finished;
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }
    }
}
