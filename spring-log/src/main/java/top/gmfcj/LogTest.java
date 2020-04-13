package top.gmfcj;


import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import top.gmfcj.config.AppConfig;
import top.gmfcj.service.IAdminService;
import top.gmfcj.service.impl.AdminService;

public class LogTest {


    public static void main(String[] args) {
        //org.apache.ibatis.logging.LogFactory.useLog4JLogging();
//        LogFactory.useCustomLogging(MyLog.class);
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
//        AdminMapper mapper = context.getBean(AdminMapper.class);
//        System.out.println(mapper.query());

//        IAdminService bean = context.getBean(IAdminService.class);
        AdminService bean = context.getBean(AdminService.class);
        bean.show();


//        Object bean = context.getBean("&logFactoryBean");
//        Object log1 = context.getBean("logFactoryBean");
//        Object log2 = context.getBean("myLog");

//        System.out.println(bean);
//        System.out.println(log1);
//        System.out.println(log1 == log2);
    }
}
