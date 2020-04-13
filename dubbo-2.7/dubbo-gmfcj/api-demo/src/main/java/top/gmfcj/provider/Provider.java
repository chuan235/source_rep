package top.gmfcj.provider;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.rpc.Protocol;
import top.gmfcj.api.DemoService;
import top.gmfcj.provider.impl.DemoServiceImpl;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class Provider {

    public static void main(String[] args) throws InterruptedException {
        ApplicationConfig applicationConfig = new ApplicationConfig("demo-provider");
        DemoService demoImpl = new DemoServiceImpl();
        // 将demoServiceImpl这个实现类注册到注册中心
        ServiceConfig<DemoService> serviceConfig = new ServiceConfig<>();
        // 设置接口
        serviceConfig.setInterface(DemoService.class);
        // 设置应用程序名称
        serviceConfig.setApplication(applicationConfig);
        // 设置实现类
        serviceConfig.setRef(demoImpl);
        // 设置注册地址
        serviceConfig.setRegistry(new RegistryConfig("192.168.222.129:2181","zookeeper"));
        // 服务提供者协议配置
//        ProtocolConfig protocolConfig = new ProtocolConfig();
//        protocolConfig.setName("dubbo");
//        protocolConfig.setPort(12345);
//        protocolConfig.setThreads(10);
        // 暴露服务
        serviceConfig.export();

        new CountDownLatch(1).await();
    }
}
