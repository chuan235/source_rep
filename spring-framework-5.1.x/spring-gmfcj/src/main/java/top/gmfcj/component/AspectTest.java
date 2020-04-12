package top.gmfcj.component;

import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class AspectTest {


	@Pointcut("@annotation(top.gmfcj.anno.AopAnno)")
	public void pointCut(){

	}

	// "+"表示IUserService的所有子类；defaultImpl 表示默认需要添加的新的类
	// 为service包下所有IUserService的实现类增加一个Log接口的增强
	// static或者非static都可以
//	@DeclareParents(value="top.gmfcj.service.*+", defaultImpl= DefaultLog.class)
//	public static Log mixnini;


//	@Before("this(log)")
//	public void beforeWriteLog(Log log){
//		log.write();
//		System.out.println("log write");
//	}



	@Before("pointCut()")
	public void before(){
		System.out.println("spring aop before");
	}



	@After("execution(* top.gmfcj.cycle.*.*(..)))")
	public void after(){
		System.out.println("spring aop after");
	}


}
