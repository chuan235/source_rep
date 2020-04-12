package top.gmfcj.test;


import top.gmfcj.component.UserTest2;
import top.gmfcj.service.impl.UserServiceImpl1;

public class ClassInstanceOfTest {


	public static void main(String[] args) {

		Object userService = new UserServiceImpl1();

		System.out.println(new UserTest2() instanceof UserTest2);

		System.out.println(userService instanceof Class);



	}

}
