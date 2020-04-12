package top.gmfcj.component;

import org.springframework.beans.factory.support.MethodReplacer;

import java.lang.reflect.Method;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2019-09-10 08:52
 */
//@Configuration
//@Component
//@Scope(proxyMode= ScopedProxyMode.TARGET_CLASS)
public class UserTest1 implements MethodReplacer {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public UserTest1 (){
		System.out.println("User test 1");
	}

	/**
	 * 替换一些已经存在的方法
	 * @param obj the instance we're reimplementing the method for
	 * @param method the method to reimplement
	 * @param args arguments to the method
	 * @return
	 * @throws Throwable
	 */
	@Override
	public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
//		if(method.getName().equals("getName")){
//			return "UserTest1 reimplement getName method ";
//		}else{
//		}
		return method.invoke(obj, args);

	}


	@Override
	public String toString() {
		return "UserTest{" +
				"name='" + name + '\'' +
				'}';
	}
}
