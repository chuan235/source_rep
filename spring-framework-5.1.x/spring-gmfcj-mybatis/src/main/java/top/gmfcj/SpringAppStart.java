package top.gmfcj;


import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.gmfcj.bean.BookInfo;
import top.gmfcj.config.AppConfig;
import top.gmfcj.mapper.BookInfoMapper;
import top.gmfcj.service.BookInfoService;

import java.util.Arrays;

public class SpringAppStart {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
//		BookInfoMapper mapper = context.getBean(BookInfoMapper.class);
//		System.out.println(mapper.selectAllBook());

		BookInfoService bean = context.getBean(BookInfoService.class);
		System.out.println(bean.selectAll());

//		Arrays.stream(BookInfo.class.getDeclaredMethods()).forEach(m -> {
//			System.out.println(m.getName() +"  "+m.getDeclaringClass());
//		});
	}
}
