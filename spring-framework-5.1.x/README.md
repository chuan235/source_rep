### BeanDefinition
    + 定义了bean的全类名
    + 定义了bean在容器中的配置，scope, lifecycle callbacks...
    + 维护了内部的依赖bean对象
    + 定义了修改bean中属性的方法，用于在bean初始化之后修改bean中的属性值

> xml中可以通过name属性指定多个别名，每个别名之间可以使用逗号，分号或者空格
```text
If you want to introduce other aliases for the bean, 
you can also specify them in the name attribute, separated by a comma (,), semicolon (;), or white space.
```  
  
> 使用alias标签定义别名    
```xml
<alias name="myApp-dataSource" alias="subsystemA-dataSource"/>
<alias name="myApp-dataSource" alias="subsystemB-dataSource"/>
```    

### 构造方法和工厂方法创建bean
```xml
<beans>
    <!--根据构造函数的参数名称配置-->
    <bean id="beanOne" class="x.y.ThingOne">
        <constructor-arg ref="beanTwo"/>
        <constructor-arg ref="beanThree"/>
    </bean>
    <bean id="beanTwo" class="x.y.ThingTwo"/>
    <bean id="beanThree" class="x.y.ThingThree"/>
    <!--根据构造函数的参数索引配置-->
    <bean id="beanOneCopy1" class="x.y.ThingOne">
        <constructor-arg index="0" ref="beanTwo"/>
        <constructor-arg index="1" ref="beanThree"/>
    </bean>
    <!--根据构造函数的参数类型配置-->
    <bean id="beanOneCopy2" class="x.y.ThingOne">
        <constructor-arg type="x.y.ThingTwo" ref="beanTwo"/>
        <constructor-arg type="x.y.ThingThree" ref="beanThree"/>
    </bean>
    <!--工厂方法，静态方法-->
    <bean id="clientService" class="examples.ClientService" factory-method="createInstance"/>
    <bean id="serviceLocator" class="examples.DefaultServiceLocator" />
    <bean id="clientService" factory-bean="serviceLocator" factory-method="createClientServiceInstance"/>
    <!--注入null-->
    <bean class="ExampleBean">
        <property name="email"><null/></property>
    </bean>
</beans>
```
注解方法
```java
@ConstructorProperties({"beanTwo", "beanThree"})
public ThingOne(ThingTwo beanTwo,ThingThree beanThree){}//...
```

### 构造方法注入和set注入的选择

```text
在spring中可以混合使用基于构造函数和基于set方法的注入
    所以对于强制依赖项使用构造函数，对于可选依赖项使用setter方法或配置方法
注意，在setter方法上使用@Required注释可以使属性成为必需的依赖项。但是，这种情况下最好使用带有参数编程式验证的构造函数注入。
    构造函数注入的优点：
        保证对象中的依赖不为空
        保证对象中的依赖已经完全初始化完成
    set方法注入的优点：
        注入的对象灵活可选，无法避免null值的检查
        使用@Primary可以提高bean在被其他对象装配时的优先级
    处理第三方的类时，灵活选用注入方法。
    spring中依赖维护的方法：xml、java配置或者注解 (xm+javaCode)/(annotation+javaCode)
 依赖的解析过程   
    依赖存在的形式：属性、构造参数、工厂方法参数
    每一个属性或者构造参数都是一个实际的值的定义，或者是对容器中另外一个bean的引用
    Each property or constructor argument is an actual definition of the value to set, or a reference to another bean in the container.
    简单类型在注入时可以自动转换，such as int, long, String, boolean, and so forth.
```
### 循环依赖问题

    + 在创建容器时，spring会初始化bean的配置，每一个会被初始化的bean都会作为一个bd存在于spring中。
    + 在bean对象创建之前，bean本身的属性，不会被设置。
    + 在创建容器时，会创建单例bean对象，将将其设置为预实例化(pre-instantiated)，只有当bean对象的依赖构建完成之后，才会完全创建成功

> 如果在A的构造函数中依赖B，在B的构造函数中依赖A，那么spring就断定这两个类是循环引用关系，抛出 BeanCurrentlyInCreationException。这种情况可以通过set方法注入来解决
> 在依赖注入之前需要完全配置好将要注入的bean，比如在A中依赖了B，那么spring在将B注入A之前会完全初始化B，之后才会使用set方法，完成属性注入

### 自动装配
优点:
    
    + 减少构造函数参数和指定属性的需要
    + 自动配置可以自动更新配置信息，如果向类中添加依赖项，直接写一个属性+Autowired注解即可完成自动装配
自动装配模式(并不是自动装配的技术)：
    
    + no
    + byName
    + byType
    + constructor
```text
    no：默认的自动装配模式，意思是不进行自动装配，必须由ref标签或者@Autowired等注解 来维护依赖关系
    byName：根据属性的名称来进行自动装配，比如setMaster => beanName=master的bean对象
    byType：根据属性的类型进行自动装配，多个同类型的存在会抛出异常，没有类型的bean就不会进行注入，属性也不会被设置
    constructor：本质和byType模式相同，只是应用在构造函数的参数上，如果构造函数中存在一个参数类型不存在于容器中，那么就会发生错误
```
自动装配的优点和缺点
    
    + 减少属性和构造器参数的指派
    + 自动装配可以在解析对象时更新配置
    + 自动配置会被property和constructor-arg中配置的值覆盖，无法对基本数据类型和String、Class类型进行自动装配(受限于设计)
    + 自动配置没有显示配置更直观
    + spring容器中的文档生成工具无法使用装配的具体信息，对于装配的类型，spring是动态推断的，而不是静态固定的
    + 对于只需要单一值的依赖来将，如果存在匹配多个bean定义的情况下，需要做出限制
    
```text
beans使用 autowire-candidate=false 来避免自动装配
beans使用 default-autowire-candidates="*Service,abc*" 来限制自动装配的bean，匹配成功的bean可以进行自动装配
```

> 实现 ApplicationContextAware 接口，使用applicationContext的API来进行属性注入

+ 解决了在一个singleton类型的Bean中持有prototype类型的bean

```java
public class CommandManager implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    public Object process(Map commandState) {
        Command command = createCommand();
        // ...
    }
    protected Command createCommand() {
        return this.applicationContext.getBean("command", Command.class);
    }
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
```
> 使用@Lookup注入
```java
public abstract class CommandManager {
    public Object process(Object commandState) {
        MyCommand command = createCommand();
        // 必须是抽象类...
    }
    @Lookup
    protected abstract MyCommand createCommand();
}
```
### Bean Scope
    + singleton  默认的作用域，在spring容器中只存在一个该类型的实例
    + prototype  存在任意数量的实例，每get一次就会创建一个新的实例
    + request    生命周期定义在单个HTTP请求中，每一个HTTP请求都用于自己的实例对象，在web环境中生效
    + session    定义在单个HTTP session中有效，在web环境生效
    + application 定义在整个ServletContext中，在web环境生效
    + websocket  bean定义作用于websocket的生命周期中，在web环境生效
> 怎么将一个小作用域的bean注册到大的作用域bean中
```text
将request作用域的bean注册到application作用域的bean，可以使用AOP 代理，spring支持将一个代理对象注入到bean中
    使用AOP创建一个rBean的代理对象，将这个代理对象注入到aBean中
<bean id="userPreferences" class="com.something.UserPreferences" scope="session">
    <!-- step1:指定容器需要对这个bean进行代理 proxy-target-class指定是否使用CGLIB动态代理技术，true表示使用CGlib动态代理 -->
    <aop:scoped-proxy proxy-target-class="false"/> 
</bean>
<!-- step2:需要使用时，直接注入即可 在单例bean中注入session作用域的bean -->
<bean id="userManager" class="com.something.UserManager">
   <property name="userPreferences" ref="userPreferences"/>
</bean>
```    
### Bean的生命周期回调
    + InitializingBean and DisposableBean interface
    + @PostConstruct and @PreDestroy 
    + config init-method and destroy-method
    + 在beans标签中指定 default-init-method or default-destroy-method
调用机制或者顺序
```text
如果存在多种初始化或者销毁方法，他们的执行顺序如下：
    1、Methods annotated with @PostConstruct
    2、afterPropertiesSet() as defined by the InitializingBean callback interface
    3、A custom configured init() method
销毁回调方法顺序一致:
    1、Methods annotated with @PreDestroy
    2、destroy() as defined by the DisposableBean callback interface
    3、A custom configured destroy() method
```
> bean全局的生命周期回调
    > org.springframework.context.Lifecycle
```text
Lifecycle定义spring容器对象的生命周期，任何Spring管理的对象都可以实现这个接口，当ApplicationContext接收到容器启动或者停止的信号时
spring容器会找出容器上下文中所有实现了LifeCycle及其子接口的实现类，并一一调用这些实现类中的对应方法
上述的过程是通过生命周期处理器 LifecycleProcessor 来实现了
Lifecycle的缺点：lifecycle接口的实现类只会在容器显示的调用start或者stop时才会去自动执行类中的接口方法
    applicationContext.start();
    applicationContext.close();
自动的生命周期扩展 SmartLifecycle 
    如果容器没有显示的调用start或者destory等方法，但也需要触发生命周期回调，就可以实现 SmartLifecycle 接口
    SmartLifecycle 接口默认时隐式自动调用

note1：stop通知不能保证在销毁之前到达。在常规关闭时，所有生命周期bean在传播一般的销毁回调之前首先收到一个停止通知。
    但是，在上下文生命周期内的热刷新或中止的刷新尝试时，只调用destroy方法。
note2：SmartLifecycle 接口定义了一个属性 phase 。
    在start通知发出后，getPhase方法返回的值越小，越先执行
    在stop通知发出后，getPhase方法返回的值越大，越先执行
    getPhase方法为 MAX_VALUE 时，这个SmartLifecycle将会在start的时候最后执行，在stop时最先执行
    SmartLifecycle接口中默认getPhase方法的返回值为 Integer.MAX_VALUE，宁外对于任何没有实现SmartLifecycle接口的普通Lifecycle类，他们的默认phase是0
note3：LifecycleProcessor中定义的 onRefresh 和 onClose 方法，也会在 applicationContext显示调用refresh和close方法时被调用
    context.start();
    context.refresh();
    context.stop();
    context.close();
```
### ApplicationContextAware and BeanNameAware
    + ApplicationContextAware接口向外暴露了ApplicationContext的具体实现对象，也可以直接使用@Autowired注入SpringApplication对象
    我们可以在这个实现类中操作 ApplicationContext 对象，访问文件资源、发布事件、访问messageSource...
    + BeanNameAware 接口中的 setBeanName 方法将在bean对象装配属性完成之后执行但是先于 InitializingBean接口的afterPropertiesSet或者自定义的init方法
    + 更多的aware接口参考 https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#aware-list
```java
// 可以自动装配的接口 AbstractApplicationContext#prepareBeanFactory
    BeanFactory
    ResourceLoader
    ApplicationEventPublisher
    ApplicationContext
    Environment
    Map<String, Object> systemProperties
    Map<String, Object> systemEnvironment
    
// BeanNameAware接口调用的位置：AbstractAutowireCapableBeanFactory#initializeBean
// 在bean装配之后，在init方法之前
```

#### @Configuration 与 @Component 中的@Bean的区别？ 普通方法的@Bean和静态方法的@Bean ？

[Concepts:@Configuration and @Bean](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#beans-java-basic-concepts)

> full与lite?
```java
if (isFullConfigurationCandidate(metadata)) {
    // is Configuration annotation => full
    beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_FULL);
}else if (isLiteConfigurationCandidate(metadata)) {
    // has Component ComponentScan Import ImportResource Bean annotation => lite
    beanDef.setAttribute(CONFIGURATION_CLASS_ATTRIBUTE, CONFIGURATION_CLASS_LITE);
}else {
    return false;
}
```
+ @Configuration 注解被称为全注解,全注解和lite注解的区别？
    > 区别就在于在ConfigurationClassPostProcessor这个类的postProcessBeanFactory方法中，这个方法是在容器refresh的时候调用的
    在refresh方法中调用这个方法之前已经调用的这个类的postProcessBeanDefinitionRegistry方法，这个方法就完成了容器中所有bean的扫描
    而在postProcessBeanFactory方法中，会对所有的bd进行筛选，对于全注解的bd，会修改这个bd的class，也就是说将bd的对应的class修改
    通过Cglib代理的ConfigurationClassEnhancer对象对原bd中的class生成了一个代理class，并将这个代理class替换了原有的class
    这样spring在后续创建bean对象的时候，就会创建这个代理对象。
+ 创建代理的目的？
    > 主要是为每一个全注解的bean设置了三个前置处理器，就是说在调用这个全注解类中的任意一个方法(成员方法)时，首先会执行这些拦截器方法
    这些拦截器会拦截类中所有的成员方法，判断方法的返回值类型的bean是否存在于容器之中，如果存在容器之中，那么就不会调用这个方法，会直接返回容器中的bean对象
    对于这个类中的静态方法，在解析这个@Bean的时候，修改这个@Bean bd的beanClass
+ 为什么使用CGlib代理？
    > jdk代理只能基于接口进行代理，而内部的一些@Bean组件不可能在接口中声明。从而采用了cglib代理
    因此就要求配置类可以被继承，并且配置类中的@Bean成员方法可以被重写


```java
class ConfigurationClassEnhancer {
    private static final Callback[] CALLBACKS = new Callback[] {
        // 判断方法的返回值对象是否存在容器之中，保证bean对象单例的特性，拦截器正对对象而来的，如果调用的是静态方法，那么这些拦截器就不会执行
        new BeanMethodInterceptor(),
        // 为代理对象设置 $$beanFactory 属性值，代理对象实现了EnhancedConfiguration接口，这个接口继承了BeanFactoryAware接口
        new BeanFactoryAwareMethodInterceptor(),
        NoOp.INSTANCE
    };
}
```
#### static @Bean and member @Bean build eanDefinition
```text
if (metadata.isStatic()) {PlatformTransactionManager 
    // static @Bean method 静态方法 修改className为Configuration注解的class
    beanDef.setBeanClassName(configClass.getMetadata().getClassName());
    // 修改工厂方法，根据构造方法的参数动态调用
    // 无参就使用无参的这个工厂方法，构造方法有参数会将参数传递到共产方法中
    // 这里其实就指定了一个静态工厂方法 通过上面的beanClassName 和这个工厂方法就完成了对象的创建
    beanDef.setFactoryMethodName(methodName);
}
else {
    // instance @Bean method 如果这个@Bean注解返回的是一个FactoryBean
    // 那么需要单独设置它的名称(参考在Configuration配置的代理时，会为这个FactoryBean的新生成一个代理对象...)
    beanDef.setFactoryBeanName(configClass.getBeanName());
    // 设置了唯一的工厂方法，创建时回调用这个方法，而不是直接去new
    beanDef.setUniqueFactoryMethodName(methodName);
}
```
> 注意使用@Bean来声明BeanPostProcessor和BeanFactoryPostProcessor，应该声明为`static @Bean`。这种组件不应该涉及到配置类的初始化。





   