package top.gmfcj.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;

@Configuration
@ComponentScan("top.gmfcj")
public class AppConfig {

//	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//		System.out.println("----------------");
//	}

//	@Bean
//	public FastJsonHttpMessageConverter jsonHttpMessageConverter() {
//		return new FastJsonHttpMessageConverter();
//    }

//	@Bean
//	public HttpMessageConverter messageConverter() {
//	}


}
