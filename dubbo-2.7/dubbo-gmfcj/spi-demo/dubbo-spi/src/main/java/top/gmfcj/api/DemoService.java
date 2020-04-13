package top.gmfcj.api;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.SPI;

/**
 * 测试注入的接口
 */
@SPI
public interface DemoService {

    @Adaptive("demo")
    public String say(String content, URL url);

    public void eat();
}
