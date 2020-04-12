package top.gmfcj.anno;


import org.springframework.context.annotation.Import;
import top.gmfcj.conf.MyBeanDefinitionRegistrar;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Import(MyBeanDefinitionRegistrar.class)
public @interface ComponentScanGmfcj {

	String[] basePackage() default "";
}
