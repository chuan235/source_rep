package top.gmfcj.provider.impl;

import top.gmfcj.api.HelloService;

public class HelloXmlService implements HelloService {
    @Override
    public void replyHello() {
        System.out.println("hello xml dubbo!");
    }

    @Override
    public String sayHello(String content) {
        System.out.println(content);
        return "hello consumer";
    }
}
