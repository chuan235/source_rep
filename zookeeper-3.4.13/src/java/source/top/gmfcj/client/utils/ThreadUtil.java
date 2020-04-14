package top.gmfcj.client.utils;


import java.util.concurrent.TimeUnit;

public class ThreadUtil {

    public static void sleep(long millis){
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
            System.out.println("sleep end");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
