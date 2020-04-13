package top.gmfcj.config;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import javax.sql.DataSource;
import java.io.Reader;

/**
 * @description:
 * @author: GMFCJ
 * @create: 2019-09-12 09:13
 */
@Configuration
@ComponentScan("top.gmfcj")
@MapperScan("top.gmfcj.mapper")
// 开启AOP自动代理
@EnableAspectJAutoProxy/*(proxyTargetClass = true)*/
public class AppConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
//        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
//        factoryBean.setMapperLocations(resolver.getResources("classpath:mapper/*.xml"));
//        factoryBean.setConfigLocation(resolver.getResource("classpath:mapper/*.xml"));
//        factoryBean.setConfigLocation(resolver.getResource("classpath:mybatis-config.xml"));
        factoryBean.setDataSource(dataSource);
        return factoryBean.getObject();
    }
    @Bean
    public DataSource dataSource(){
        PooledDataSource dataSource = new PooledDataSource();
        dataSource.setDriver("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/library?useUnicode=true&useSSL=true&characterEncoding=UTF-8");
        dataSource.setUsername("root");
        dataSource.setPassword("password");
        return dataSource;
    }
}
