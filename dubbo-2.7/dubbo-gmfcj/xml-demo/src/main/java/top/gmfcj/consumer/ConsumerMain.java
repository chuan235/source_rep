package top.gmfcj.consumer;


import org.springframework.context.support.ClassPathXmlApplicationContext;
import top.gmfcj.api.HelloService;

import java.util.concurrent.CountDownLatch;

public class ConsumerMain {

    public static void main(String[] args) throws InterruptedException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:dubbo-consumer.xml");
        context.start();
        HelloService service = (HelloService) context.getBean("helloService");
        // 会进入mockClusterInvoker
        String result = service.sayHello("xml-demo hello");
        System.out.println(result);
        new CountDownLatch(1).await();
    }
}
