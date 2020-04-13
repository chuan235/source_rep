package top.gmfcj.demo;

import org.apache.dubbo.common.Extension;
import org.apache.dubbo.common.URL;
import top.gmfcj.api.DemoService;

public class DemoServiceImpl1 implements DemoService {
    @Override
    public String say(String content, URL url) {
        return content + "impl 1";
    }

    @Override
    public void eat() {
        System.out.println("implement1 eat");
    }
}
