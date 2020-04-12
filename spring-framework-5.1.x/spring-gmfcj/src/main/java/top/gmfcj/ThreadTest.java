package top.gmfcj;

import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2020-04-08 16:49
 */
public class ThreadTest {

	public static void main(String[] args) {
		ThreadTest test = new ThreadTest();
		List<String> list = test.select();
		doSomething();

		System.out.println(list.size());


	}

	private static void doSomething() {
		ThreadTest test = new ThreadTest();
		List<String> list2 = test.select();
		list2.add("fqenfoiq");
		System.out.println(list2.size());
	}


	public List<String> select(){
		List<String> rs = new ArrayList<>();
		rs.add("123");
		return rs;
	}
}
