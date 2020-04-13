package top.gmfcj.demo;

import org.apache.dubbo.common.URL;
import top.gmfcj.api.DemoService;

public class IndexService implements DemoService {

    private DemoService demoService;

    public void setDemoService(DemoService demoService) {
        this.demoService = demoService;
    }

    @Override
    public String say(String content, URL url) {
        System.out.println("干哈呢？");
        return demoService.say(content, url);
    }

    @Override
    public void eat() {
        System.out.println("index eat");
    }
}
