package top.gmfcj.demo;

import org.apache.dubbo.common.URL;
import top.gmfcj.api.DemoService;

public class DemoServiceImpl2 implements DemoService {
    @Override
    public String say(String content, URL url) {
        return content + "impl 2";
    }

    @Override
    public void eat() {
        System.out.println("implement2 eat");
    }
}
