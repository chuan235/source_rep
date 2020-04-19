package top.gmfcj.zkmt;

import org.apache.zookeeper.common.Time;
import org.junit.Test;

public class SessionTest {

    @Test
    public void testNextExpirationTime() {
        long expirationInterval = 2000;
        long time = Time.currentElapsedTime();
        System.out.println("current time = "+time);
        System.out.println("next expiration time = " + (time / expirationInterval + 1) * expirationInterval);
    }
}
