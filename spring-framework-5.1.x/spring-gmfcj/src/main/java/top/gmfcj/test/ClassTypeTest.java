package top.gmfcj.test;

/**
 * @description: classMetaData中类的划分 => 划分为五种类
 * @author: GMFCJ
 * @create: 2019-09-14 10:11
 * ClassTypeTest => top level class 顶部类
 * NestedClass   => nested class 静态内部类
 * InnerClass	 => inner class 非静态内部类
 * LocalClass	 => local class 在方法中定义的类
 * AnonymousClass=> anonymous class 匿名类
 *
 */
public class ClassTypeTest {

	public class InnerClass{

	}
	public static class NestedClass{

	}

	public static void main(String[] args) {
		class LocalClass{

		}
		new Thread(() -> {
			System.out.println("anonymous class");
		}).start();
	}
}
