package top.gmfcj.consumer;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import top.gmfcj.api.DemoService;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

/**
 * 消费服务
 */
public class ComsumerService {

    public static void main(String[] args) throws InterruptedException {
        ApplicationConfig application = new ApplicationConfig("demo-consume");
        // 引用远程服务
        ReferenceConfig<DemoService> reference = new ReferenceConfig<>();
        // 设置接口，设置注册中心
        reference.setInterface(DemoService.class);
        // 设置应用程序名称
        reference.setApplication(application);
        // 设置注册地址
        reference.setRegistry(new RegistryConfig("192.168.222.129:2181","zookeeper"));
        // 获取暴露的服务
        DemoService service = reference.get();
        System.out.println(service.sayHello("consume exec demoService"));

        new CountDownLatch(1).await();
    }
}
