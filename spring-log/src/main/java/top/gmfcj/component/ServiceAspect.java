package top.gmfcj.component;

import org.aopalliance.intercept.Joinpoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ServiceAspect {

    /**
     * 匹配service包下的类
     */
    @Pointcut("execution(* top.gmfcj.service.impl.*.*(..))")
    public void pointCut(){}


    @Before("pointCut()")
    public void beforeLog(){
        System.out.println("before exec method");
    }
}
