package top.gmfcj.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyFactory {


    /**
     * @param clazz 实现类的class
     * @return 代理对象
     */
    @SuppressWarnings("unchecked")
    public static Object buildTranscationProxy(Class clazz, Object... args) {
        Object target = null;
        try {
            if (args.length == 0) {
                target = clazz.newInstance();
            } else {
                Class[] paramTypes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    paramTypes[i] = args[i].getClass();
                }
                Constructor constructor = clazz.getDeclaredConstructor(paramTypes);
                target = constructor.newInstance(args);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (target == null) {
            throw new IllegalArgumentException("构造方法错误");
        }
        return Proxy.newProxyInstance(clazz.getClassLoader(), clazz.getInterfaces(), new TranscationHandler(target));
    }

    @SuppressWarnings("unchecked")
    public static boolean hasTransactionAnnotation(Class targetClass, Method method) throws Exception{
        // 检查接口和类上是否在注解
        if (targetClass.isAnnotationPresent(MyTransaction.class)) {
            return true;
        }
        for (Class anInterface : targetClass.getInterfaces()) {
            if (anInterface.isAnnotationPresent(MyTransaction.class)) {
                return true;
            }
            // 接口上的方法
            Method interfaceMethod = anInterface.getMethod(method.getName(), method.getParameterTypes());
            if(interfaceMethod != null && interfaceMethod.isAnnotationPresent(MyTransaction.class)){
                return true;
            }
        }
        // 类的方法
        Method targetMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());
        if (targetMethod.isAnnotationPresent(MyTransaction.class)) {
            return true;
        }
        return false;
    }

}
