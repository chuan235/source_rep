package top.gmfcj.service.impl;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.gmfcj.service.IUserService;

@Service
public class UserServiceImpl1 implements IUserService, DisposableBean {


//	@Autowired // 可以检测到这里的注解 和@Required相同
//	public UserServiceImpl(/*@Autowired 这里的注解检测不到*/ UserTest1 userTest1){
//
//	}

//	public UserServiceImpl(){
//		System.out.println("no param constructor");
//	}

//	public UserServiceImpl(UserTest1 userTest1){
//		System.out.println("has usertest1 param constructor");
//	}

	@Transactional
	@Override
	public void query() {
		System.out.println("userService1  query");
	}

	@Override
	public void destroy() throws Exception {
		System.out.println("usersERVICE  destroy--------------");
	}


//	@PreDestroy
//	public void preDes(){
//		System.out.println("usersERVICE  destroy--------------");
//	}
}
