package top.gmfcj.config;

import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.gmfcj.interceptor.MyInterceptor;

import java.util.List;

@EnableWebMvc
@Configuration
@ComponentScan("top.gmfcj")
public class AppConfig {





//	@Bean
//	public HttpMessageConverter messageConverter() {
//	}


}
