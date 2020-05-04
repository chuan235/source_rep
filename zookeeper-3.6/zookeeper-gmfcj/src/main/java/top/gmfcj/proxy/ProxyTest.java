package top.gmfcj.proxy;


import java.util.concurrent.CountDownLatch;

public class ProxyTest {

    public static void main(String[] args) throws InterruptedException {

        IUserService userService = (IUserService) ProxyFactory.buildTranscationProxy(UserServiceImpl.class);
        userService.findAll();
        new CountDownLatch(1).await();

    }
}
