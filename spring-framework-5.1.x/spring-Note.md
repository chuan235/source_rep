## [Aware List](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#aware-list)

+ Aware interfaces
```text
1、可以自动装配的aware
ApplicationContextAware
ApplicationEventPublisherAware
ResourceLoaderAware
BeanFactoryAware
2、为对象服务的aware
BeanNameAware
BeanClassLoaderAware
3、aspectJ 服务的aware
LoadTimeWeaverAware
4、资源适配器，引导容器运行，用于JCA CCI
BootstrapContextAware
5、JMX发布aware
NotificationPublisherAware
6、SpringMVC装配servletConfig和ServletContext
ServletConfigAware
ServletContextAware
```

## [Spring容器的扩展点](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#beans-factory-extension)

+ BeanPostProcessor => 在init之前和之后执行 示例：AutowiredAnnotationBeanPostProcessor、CommonAnnotationBeanPostProcessor、AnnotationAwareAspectJAutoProxyCreator
    + 子类 InstantiationAwareBeanPostProcessor 
    + 操作实例化后的bean
+ BeanFactoryPostProcessor => refresh 中 invokeBeanFactoryPostProcessors 方法执行所有的 BeanFactoryPostProcessor 实现类
    + 子类 BeanDefinitionRegistryPostProcessor
    + 操作bean的元数据信息bd，从这个接口中可以拿到 beanFactory 示例：ConfigurationClassPostProcessor
+ FactoryBean => 每一个实现了FactoryBean接口的类，都会在spring中存在两个对象，一个getObject返回的对象(myFactoryBean)，一个是实现了FactoryBean接口的对象(&myFactoryBean)
    + 示例：SqlSessionFactoryBean、MapperFactoryBean、ThreadPoolExecutorFactoryBean
+ CustomAutowireConfigurer => 使用自己的注解实现Qualifier注解的效果    

## Spring Environment 

+ profiles => 环境命名空间，如果指定了profile，只有在这个profile下的bean才会被纳入spring管理
+ properties => 环境属性参数，app属性文件、JVM环境参数、系统环境变量、JNDI、servlet上下文参数、特别属性对象、映射对象...为用户提供一个方便的服务接口，用于配置属性源并从中解析属性。

### Profile

```java
@Profile("development")
@Profile("!development")
@Profile("development | production")
@Profile("development & production")
@Profile({"p1", "!p2"})
@Profile("production & (us-east | eu-central)")
```
```text
// 激活环境 javacode
AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
ctx.getEnvironment().setActiveProfiles("development");
// properties
spring.profiles.active=development
// 启动参数
-Dspring.profiles.active="development"
// 如果不配置，会启用默认的profile
@Profile("default")
```
### PropertySource 

> 环境参数的优先级,以web环境为例
```text
1、ServletConfig parameters (if applicable — for example, in case of a DispatcherServlet context)
2、ServletContext parameters (web.xml context-param entries)
3、JNDI environment variables (java:comp/env/ entries)
4、JVM system properties (-D command-line arguments)
5、JVM system environment (operating system environment variables)
如果存在key相同的配置，前面的会覆盖后面的配置
```
> 使用系统环境变量
```java
@Configuration
@PropertySource("classpath:/com/myco/app.properties")
public class AppConfig {
    @Autowired // 可以自动注入
    Environment env;
    
    @Value("${testbean.name}")
    private String beanName;
    
    @Bean
    public TestBean testBean() {
        TestBean testBean = new TestBean();
        // 手动通过环境对象get
        testBean.setName(env.getProperty("testbean.name"));
        return testBean;
    }
}
```
### LoadTimeWeaver 
> Spring使用LoadTimeWeaver在类加载到Java虚拟机(JVM)时动态改变这些class
> 在配置类上使用 `@EnableLoadTimeWeaving` 注解会向容器中注入一个 LoadTimeWeaver 对象。
    此后任何bean都可以实现LoadTimeWeaverAware，从而接收对加载时编织器实例的引用

### MessageSource...
    + MessageSource接口访问国际化信息
    + ResourceLoader加载各种各样的资源文件
    + ApplicationEventPublisher 发布Application事件
    + HierarchicalBeanFactory 接口去加载多个hierarchical contexts(分层的上下文 web...)

```text
ApplicationContext 这个接口继承了MessageSource接口，因此context提供了国际化的功能
同时Spring也提供了 HierarchicalMessageSource 去解析 message上下文。主要提供的API如下
String getMessage(String code, Object[] args, String default, Locale loc) 
String getMessage(String code, Object[] args, Locale loc) 找不到抛出异常 NoSuchMessageException 
String getMessage(MessageSourceResolvable resolvable, Locale locale) resolvable内部就有code args loc 等参数

当ApplicationContext被加载时，会自动去寻找容器中 MessageSource 类型的bean，这个beanName必须为messageSource
    如果找到了这样的一个bean，就会将上述方法委托给这个对象
    如果没找到，就会从父容器中寻找
    还是没找到，就会创建一个 DelegatingMessageSource 对象，来接收上述方法的调用
    
Spring内部提供了两个 MessageSource 实现类,这两个实现类都实现了 HierarchicalMessageSource 接口以实现嵌套消息传递
    StaticMessageSource 很少使用，但是提供了可以添加message的功能
    ResourceBundleMessageSource 以下测试使用

public static void main(String[] args) {
    // 直接转换
    MessageSource resources = new ClassPathXmlApplicationContext("beans.xml");
    String message = resources.getMessage("message", null, "Default", Locale.ENGLISH);
    System.out.println(message);
}
```

### Standard and Custom Events
    + ContextRefreshedEvent => refresh方法：所有bean、bean后置处理器已经加载完成，context已经创建完成
    + ContextStartedEvent => 所有的生命周期bean收到一个显示的启动信号
    + ContextStoppedEvent => 所有的生命周期bean收到一个显示的停止信号
    + ContextClosedEvent  => 所有的singleton将会被销毁，一旦context关闭将说明context走到了声明周期的终点无法刷新或者重启
    + RequestHandledEvent => 一个web事件，通知所有bean HTTP请求已得到服务。此事件在请求完成后发布。仅适用于使用Spring的DispatcherServlet的web应用程序
    + ServletRequestHandledEvent => RequestHandledEvent的子类，向servletContext添加信息

> XmlWebApplicationContext 支持热刷新而 GenericApplicationContext 不支持

```java
// 注解版的自定义监听器
@EventListener({ContextStartedEvent.class, ContextRefreshedEvent.class})
public void handleContextStart() {
    // ...
}
@EventListener// 传入一个事件
@Async//异步事件
public void processBlackListEvent(BlackListEvent event) {
    /**
    * 源码：AsyncExecutionInterceptor#invoke
     * 1、异步监听器在处理的时候如果发生了异常，它不会抛出异常，内部只是打印了错误日志：SimpleAsyncUncaughtExceptionHandler
     * 2、异步监听器不能通过返回值来发布后续事件，需要使用ApplicationEventPublisher来手动发布事件
     */
}
```

### BeanFactory or ApplicationContext 
> ApplicationContext是BeanFactory的子接口。BeanFactory只提供了一个bean工厂最基本的API
ApplicationContext的实现类GenericApplicationContext或者AnnotationConfigApplicationContext这两个类中都提供了spring容器的核心功能
    + 加载配置文件
    + 扫描bean
    + 使用程序或者注解注册bean以及5.0新增函数式注解bean
    
> 如果需要完全自定义bean的创建或者初始化流程则可以使用BeanFactory接口，因为在 AnnotationConfigApplicationContext(DefaultListableBeanFactory)
中制定好了创建和初始化的流程

[BeanFactory与ApplicationContext的对比](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#context-introduction-ctx-vs-beanfactory)

















