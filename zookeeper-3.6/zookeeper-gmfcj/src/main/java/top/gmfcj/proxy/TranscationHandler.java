package top.gmfcj.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.Connection;

public class TranscationHandler implements InvocationHandler {

    private Object target;

    public TranscationHandler(Object target){
        this.target = target;
    }
    /**
     *
     * @param proxy 代理对象
     * @param method 方法
     * @param args 方法的参数
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 判断是不是object的方法
        if (method.getDeclaringClass() == Object.class) {
            return method.invoke(target, args);
        }
        // 事务方法
        if (!method.isAccessible() && ProxyFactory.hasTransactionAnnotation(target.getClass(), method)) {
            // 获取连接
            Connection connection = DbUtil.getConnection();
            // 开启事务
            //connection.setAutoCommit(false);
            System.out.println("开启事务");
            try {
                // 调用真是的service方法
                Object result = method.invoke(target, args);
                System.out.println("提交事务");
                return result;
            } catch (Exception ex) {
                ex.printStackTrace();
                connection.rollback();
            } finally {
                //connection.close();
                System.out.println("关闭连接");
            }
        }
        return null;
    }
}
