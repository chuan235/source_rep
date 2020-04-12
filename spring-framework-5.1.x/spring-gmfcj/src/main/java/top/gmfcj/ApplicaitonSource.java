package top.gmfcj;


import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.gmfcj.conf.AppConfig;
import top.gmfcj.service.IUserService;


public class ApplicaitonSource {

	public static void main(String[] args) {
		/**
		 * AnnotationConfigApplicationContext如果传入一个class对象或者basepackage的字符串，在构造方法中就会进行容器的刷新
		 * 这一行代码就可以完成整个容器的初始化
		 * 这里自动刷新之后后续就会在refresh方法中的
		 * 		ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();中退出
		 * 这里需要进行CAS的设置值，第一刷新就已经改变了这个值，那么第二次刷新就会出现更新值错误，进而抛出一个异常
		 * 	refresh
		 * 		=> AbstractApplicationContext#obtainFreshBeanFactory()
		 * 			=> GenericApplicationContext#refreshBeanFactory()
		 * 这个异常spring并没有去catch，而是直接退出refresh方法
		 *
		 * 所以第二次刷新就不会执行在context实例后注册的BeanDefinitionRegistryPostProcessor或者BeanFactoryPostProcessor
		 *
		 * 而如果不调用这两种构造方法，其他的构造方法需要进行手动刷新，后续在refresh方法之前的中注册的类都会被放到bd的map中
		 */
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
//		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(UserTest2.class);

//
//		Log service = (Log) context.getBean("userServiceImpl1");
//		service.write();

//		IUserService service2 = (IUserService) context.getBean("userServiceImpl1");
//		service2.query();

//		context.addApplicationListener();

//		CycleTest1 bean = context.getBean(CycleTest1.class);
//		bean.hello();

		IUserService bean = context.getBean(IUserService.class);
		bean.query();


//		context.register(UserTest3.class);
//
//		UserTest3 bean = context.getBean(UserTest3.class);
//		context.register(AppConfig.class);
//		context.addBeanFactoryPostProcessor(new MyBeanFactoryPostProcessor());
//		context.addBeanFactoryPostProcessor(new MyBeanDefinitionRegistryPostProcessor());
//		context.refresh();
//		context.register(EnhancerConfig.class);
//		context.addBeanFactoryPostProcessor(new MyBeanDefinitionRegistryPostProcessor());
//		context.addBeanFactoryPostProcessor(new MyBeanFactoryPostProcessor());
//		context.refresh();

//		AppConfig bean1 = context.getBean(AppConfig.class);
//
//		UserTest2 bean = context.getBean(UserTest2.class);
//		bean.test();


//		MyBeanFactoryPostProcessor processor = context.getBean(MyBeanFactoryPostProcessor.class);


//		IUserService service1 = (IUserService)context.getBean("userService1");
//		System.out.println(service1.query().equals(bean));

//		IUserService bean = context.getBean(IUserService.class);
//		bean.query();
//
//		IUserService bean = context.getBean(IUserService.class);
//		bean.query();
		context.close();
//		EnhancerConfig bean = context.getBean(EnhancerConfig.class);


//		try {
//			// 阻塞线程
//			// java -classpath "D:\env\java\jdk8.171\lib\sa-jdi.jar" sun.jvm.hotspot.HSDB
//			System.in.read();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

}



