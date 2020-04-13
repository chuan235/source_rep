package top.gmfcj;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import top.gmfcj.api.DemoService;
import top.gmfcj.api.OperationService;
import top.gmfcj.demo.IndexService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.CountDownLatch;

/**
 *
 */
public class DubboSpiMain {

    public static void main(String[] args) {
//        ExtensionLoader<OperationService> loader = ExtensionLoader.getExtensionLoader(OperationService.class);
        // startDubboSpi(loader);
//         dubboAop(loader);
        dubboInject();
        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void dubboInject() {
        ExtensionLoader<DemoService> loader = ExtensionLoader.getExtensionLoader(DemoService.class);
        Map<String, String> params = new HashMap<>();
        params.put("demo", "impl2");
        URL url = new URL("", "", 1, params);
        DemoService index = loader.getExtension("index");
        System.out.println(index.say("hello inject", url));
    }

    public static void dubboAop(ExtensionLoader<OperationService> loader) {
        // AOP 构建wrapper
        // wrapper从上往下执行，从前向后执行，类似于静态代理
        OperationService service = loader.getExtension("plus");
        service.operation();
    }


    public static void startDubboSpi(ExtensionLoader<OperationService> loader) {
        // 这里可以输入一个true，就会返回默认的实现类（@SPI中的value对应的实现类）
        OperationService service = loader.getExtension("plus");
        service.operation();
        System.out.println("------------------------");
        OperationService division = loader.getExtension("division");
        division.operation();
    }
}
