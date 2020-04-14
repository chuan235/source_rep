package top.gmfcj.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * @Import 导入的三种类型
 * (*) 导入一个类
 * (*) 导入一个 ImportSelector
 * (*) 导入一个 ImportBeanDefinitionRegistrar
 */

//@ComponentScan(basePackages = "top.gmfcj", scopedProxy = ScopedProxyMode.TARGET_CLASS)
//@Import(MyBeanDefinitionRegistryPostProcessor.class)
//@Import(MyImportSelectors.class)
//@EnableAspectJAutoProxy
@EnableAspectJAutoProxy
@ComponentScan("top.gmfcj")
@Configuration
@EnableTransactionManagement
public class AppConfig {


	@Bean
	public DataSource dataSource(){
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName("com.mysql.jdbc.Driver");
		dataSource.setUrl("jdbc:mysql://localhost:3306/library?useUnicode=true&useSSL=true&characterEncoding=UTF-8");
		dataSource.setUsername("root");
		dataSource.setPassword("password");
		return dataSource;
	}

	@Bean
	public DataSourceTransactionManager transactionManager(@Autowired DataSource dataSource){
		DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();
		transactionManager.setDataSource(dataSource);
		return transactionManager;
	}

//	@Bean
//	public UserTest2 userTest2() {
//		return new UserTest2();
//	}
//
//	@Bean
//	public IUserService userService1() {
//		UserTest2 test2 = userTest2();
//		UserServiceImpl1 userService1 = new UserServiceImpl1();
////		userService1.setUserTest2(userTest2());
//		return userService1;
//	}

//	@Bean
//	public IUserService userService2() {
//		UserServiceImpl1 userService2 = new UserServiceImpl1();
////		userService2.setUserTest2(userTest2());
//		return userService2;
//	}
}
