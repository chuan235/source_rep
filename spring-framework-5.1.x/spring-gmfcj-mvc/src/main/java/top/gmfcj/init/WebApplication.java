package top.gmfcj.init;


import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.core.ApplicationContext;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import top.gmfcj.config.AppConfig;
import top.gmfcj.servlet.MyServlet;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;


public class WebApplication {

	/**
	 * 获取class的根路径
	 * String basePath = clazz.getResource("/").getPath();
	 * File file = createTempDir(6000);
	 * 获取系统的临时目录
	 * System.getProperty("java.io.tmpdir")
	 */
	public static void run(Class clazz){
		// 初始化spring的环境
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.register(AppConfig.class);
//		context.setServletContext(servletContext);
		// 添加@EnableWebMvc注解时需要注释掉刷新这里，不然会出现
		// Error creating bean with name 'resourceHandlerMapping' defined in DelegatingWebMvcConfiguration
		// 没有向AnnotationConfigWebApplicationContext中设置 servletContext，解决方法就是在刷新前设置servletContext或者不刷新
//		context.refresh();
		// 获取系统的临时目录
		File file = new File(System.getProperty("java.io.tmpdir"));
		// 创建tomcat，将web环境关联到tomcat上
		Tomcat tomcat = new Tomcat();
		tomcat.setPort(9090);
		Context ctx = tomcat.addContext("/", file.getAbsolutePath());
		// 创建注册servlet
		DispatcherServlet servlet = new DispatcherServlet(context);
		Tomcat.addServlet(ctx, "springmvc", servlet).setLoadOnStartup(0);
		ctx.addServletMapping("/","springmvc");
		try {
			// 启动tomcat
			tomcat.start();
			tomcat.getServer().await();
		} catch (LifecycleException e) {
			e.printStackTrace();
		}
	}

	protected static final File createTempDir(int port) {
		try {
			File tempDir = File.createTempFile("tomcat", ":" + port );
			tempDir.delete();
			tempDir.mkdir();
			tempDir.deleteOnExit();
			return tempDir;
		} catch (IOException var3) {
			var3.printStackTrace();
		}
		return null;
	}


}
