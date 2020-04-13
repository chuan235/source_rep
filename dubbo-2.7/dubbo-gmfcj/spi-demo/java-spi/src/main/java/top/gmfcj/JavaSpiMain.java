package top.gmfcj;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * java.util.ServiceLoader
 *  路径必须是：META-INF/services/
 *  String fileName = PREFIX + service.getName();  META-INF/services/top.gmfcj.api.OperationService
 *
 */
public class JavaSpiMain {

    public static void main(String[] args) {
        // 核心代码，就是加载所有的类
        ServiceLoader<OperationService> services = ServiceLoader.load(OperationService.class);
        Iterator<OperationService> iterator = services.iterator();
        while(iterator.hasNext()){
            OperationService service = iterator.next();
            System.out.println(service.getClass().getName());
            service.operation();
        }

    }

    public static interface OperationService {

        public void operation();
    }
}
