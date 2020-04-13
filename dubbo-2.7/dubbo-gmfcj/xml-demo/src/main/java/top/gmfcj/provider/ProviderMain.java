package top.gmfcj.provider;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CountDownLatch;

public class ProviderMain {

    public static void main(String[] args) throws InterruptedException {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:dubbo-provider.xml");
        applicationContext.start();
        new CountDownLatch(1).await();
    }
}
