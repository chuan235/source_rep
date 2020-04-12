package top.gmfcj.component;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import top.gmfcj.service.IUserService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2019-09-10 08:52
 */
//@Component
public class UserTest3 implements ApplicationContextAware {

//	@Autowired
//	@Resource
//	private UserTest2 userTest2;

//	@Autowired
	private IUserService userService;


//	private static UserTest2 userTest2;

	private ApplicationContext applicationContext;

// 使用set方法为静态变量注入
//	@Autowired
//	public void setUserTest2(UserTest2 userTest2){
//		UserTest3.userTest2 = userTest2;
//	}

	@PostConstruct
	public void init2(){
//		UserTest3.userTest2 = applicationContext.getBean(UserTest2.class);
		System.out.println("UserTest3 @PostConstruct");
	}

	@PreDestroy
	public void destory1(){
//		userService.query();
		//userTest2.test();
		System.out.println("UserTest3 @PreDestroy");
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
