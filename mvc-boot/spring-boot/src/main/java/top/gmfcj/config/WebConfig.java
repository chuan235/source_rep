package top.gmfcj.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.gmfcj.interceptor.MyInterceptor;

import java.util.ArrayList;
import java.util.List;

@Configuration
//@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MyInterceptor())
                .addPathPatterns("/*");
    }

//    @Override
//    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//        WebMvcConfigurer.super.configureMessageConverters(converters);
//        //创建fastjson转换器实例
//        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
//        //配置对象
//        FastJsonConfig config = new FastJsonConfig();
//        List<MediaType> mediaTypes = new ArrayList<>();
//        //中文编码
//        MediaType mediaType = MediaType.APPLICATION_JSON_UTF8;
//        mediaTypes.add(mediaType);
//        config.setSerializerFeatures(SerializerFeature.PrettyFormat);
//        converter.setSupportedMediaTypes(mediaTypes);
//        converter.setFastJsonConfig(config);
//        converters.add(converter);
//    }

//    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//        registry.addViewController("/").setViewName("hello");
//        registry.addViewController("/page").setViewName("hello");
////        registry.addViewController("/test").setViewName("testPage");
//    }
}
