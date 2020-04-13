package top.gmfcj;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @SpringBootApplication(scanBasePackages={"top.gmfcj"})
 */
@SpringBootApplication
public class SpringWebApplicationStart {

    public static void main(String[] args) {
        SpringApplication.run(SpringWebApplicationStart.class);
//        SpringApplication application = new SpringApplication(SpringWebApplicationStart.class);
//        ConfigurableApplicationContext context = application.run(args);
//        for (String name : context.getBeanDefinitionNames()) {
//            System.out.println(name);
//        }
    }
}
