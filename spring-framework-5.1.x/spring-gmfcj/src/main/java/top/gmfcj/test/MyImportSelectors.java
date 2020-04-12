package top.gmfcj.test;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

/**
 *
 */
public class MyImportSelectors implements ImportSelector {

	@Override
	public String[] selectImports(AnnotationMetadata importingClassMetadata) {
		return new String[]{"top.gmfcj.test.MyBeanFactoryPostProcessor", "top.gmfcj.test.MyBeanDefinitionRegistryPostProcessor"};
	}
}
