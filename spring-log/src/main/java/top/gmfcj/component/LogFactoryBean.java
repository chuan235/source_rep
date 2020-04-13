package top.gmfcj.component;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;
import top.gmfcj.log.MyLog;

/**
 * @description: factoryBean返回两种对象
 * @author: GMFCJ
 * @create: 2019-09-13 23:54
 */
@Component
public class LogFactoryBean implements FactoryBean {


    @Override
    public Object getObject() throws Exception {
        return new MyLog();
    }

    @Override
    public Class<?> getObjectType() {
        return MyLog.class;
    }

    @Override
    public String toString() {
        return "LogFactoryBean{}";
    }
}
