package top.gmfcj.component;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class MyAppConfigCondition  implements Condition {

	@Override
	public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
		System.out.println(context);
		System.out.println(metadata);
		if (context.getBeanFactory().getBeanDefinition("appConfig")!=null) {
			return true;
		}
		if (metadata.getClass().isInterface()) {
			return false;
		}
		return true;
	}
}
