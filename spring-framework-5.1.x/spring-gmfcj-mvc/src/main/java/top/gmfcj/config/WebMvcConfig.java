package top.gmfcj.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.gmfcj.component.MyAppConfigCondition;
import top.gmfcj.interceptor.MyInterceptor;

import java.util.List;

@Conditional(MyAppConfigCondition.class)
@Configuration
public class WebMvcConfig  implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new MyInterceptor()).addPathPatterns("/index/2");
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		converters.add(jsonHttpMessageConverter());
	}

	@Bean
	public FastJsonHttpMessageConverter jsonHttpMessageConverter() {
		return new FastJsonHttpMessageConverter();
	}

}
