package top.gmfcj.jvmm;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @description: 1.8 HashMap死循环
 */
public class HashMap8 {
    public static void main(String[] args) {
        HashMapThread hmt0 = new HashMapThread();
        HashMapThread hmt1 = new HashMapThread();
        HashMapThread hmt2 = new HashMapThread();
        HashMapThread hmt3 = new HashMapThread();
        HashMapThread hmt4 = new HashMapThread();
        HashMapThread hmt5 = new HashMapThread();
        HashMapThread hmt6 = new HashMapThread();
        HashMapThread hmt7 = new HashMapThread();
        HashMapThread hmt8 = new HashMapThread();
        HashMapThread hmt9 = new HashMapThread();
        HashMapThread hmt10 = new HashMapThread();
        HashMapThread hmt11 = new HashMapThread();
        HashMapThread hmt12 = new HashMapThread();
        hmt0.start();
        hmt1.start();
        hmt2.start();
        hmt3.start();
        hmt4.start();
        hmt5.start();
        hmt6.start();
        hmt7.start();
        hmt8.start();
        hmt9.start();
        hmt10.start();
        hmt11.start();
        hmt12.start();
    }

}

class HashMapThread extends Thread {
    private AtomicInteger ai = new AtomicInteger(0);
    private static Map<Person, Integer> map = new HashMap<>(1);

    @Override
    public void run() {
        while (ai.get() < 5000) {
            map.put(new Person(ai.get(), "name" + Thread.currentThread().getName()), ai.get());
            ai.incrementAndGet();
        }
        System.out.println(Thread.currentThread().getName() + "执行结束完" + ai.get());
    }
}