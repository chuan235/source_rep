package top.gmfcj.distribute.lock;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Ticket {

    private static Integer tickets = 1;


    public boolean buyTicket(){

        if (tickets <= 0) {
            System.out.println(Thread.currentThread().getName()+"购票失败");
            return false;
        }
        System.out.println(Thread.currentThread().getName()+"购票成功");
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        tickets--;

        return true;
    }

}
