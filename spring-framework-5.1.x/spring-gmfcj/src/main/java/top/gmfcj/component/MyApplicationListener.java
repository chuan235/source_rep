package top.gmfcj.component;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class MyApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

	@Async
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		System.out.println(" refresh event ......");
	}
}
