package top.gmfcj.conf;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2019-09-10 22:06
 */
public class MyBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {



	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {



	}
}
