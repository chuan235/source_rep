package top.gmfcj.distribute.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SystemOne implements Runnable {

    private Ticket ticket;

    private Lock lock = new ReentrantLock();
    private Lock zklock = new ZkLock();

    public SystemOne(Ticket ticket) {
        this.ticket = ticket;
    }

    @Override
    public void run() {
        // lock.lock();
        zklock.lock();
        System.out.println(Thread.currentThread().getName() +"购票结果" + ticket.buyTicket());
        // lock.unlock();
        zklock.unlock();
    }
}
