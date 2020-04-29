package top.gmfcj.proxy;


public class ProxyTest {

    public static void main(String[] args) {

        IUserService userService = (IUserService) ProxyFactory.buildTranscationProxy(UserServiceImpl.class);
        userService.findAll();

    }
}
