package top.gmfcj;


import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import top.gmfcj.config.AppConfig;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;

public class MyWebApplicationInitializer /*implements WebApplicationInitializer*/ {

//	@Override
	public void onStartup(ServletContext servletCxt) {

		AnnotationConfigWebApplicationContext ac = new AnnotationConfigWebApplicationContext();
		ac.register(AppConfig.class);
		ac.refresh();

		DispatcherServlet servlet = new DispatcherServlet(ac);
		ServletRegistration.Dynamic registration = servletCxt.addServlet("springServlet", servlet);
		registration.setLoadOnStartup(1);
		registration.addMapping("/app/*");
	}
}
