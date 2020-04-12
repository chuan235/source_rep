## [Spring Resource](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#resources)

接口定义

```java
public interface Resource extends InputStreamSource {
	// 验证资源是否存在
    boolean exists();
	// 返回资源是否具有打开流的句柄
    boolean isOpen();
	// 返回资源对应的URL handler
    URL getURL() throws IOException;
	// 返回资源对应的File handler
    File getFile() throws IOException;
	// 创建一个与资源相关的Resource对象
    Resource createRelative(String relativePath) throws IOException;
	// 返回资源文件的名称 比如:application.xml
    String getFilename();
	// 返回资源的描述，用于处理该资源时的错误输出。这通常是完全限定的文件名或资源的实际URL
    String getDescription();
}
```

实现类

```java
UrlResource // 本地文件、http文件、FTP文件
ClassPathResource// 访问类路径下的资源
FileSystemResource // 访问本地、URL文件
ServletContextResource// web路径下的资源
InputStreamResource//给定InputStream的资源实现，返回一个已经打开的InputStream，(读取一次就需要关闭一次)不建议多次读取
ByteArrayResource // 通过给定的字节数组，创建一个ByteArrayInputStream，这个可以多次使用(自己开自己关)
```

## [Spring Validation](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#validator)

spring验证

接口API

```java
// org.springframework.validation.Validator
// 指定验证哪一种class
boolean supports(Class<?> clazz);
// 验证的具体逻辑
void validate(Object target, Errors errors);
```

示例

```java
public class Person {
    private String name;
    private int age;
    // the usual getters and setters...
}
```

```java
public class PersonValidator implements Validator {
    private Validator personValidator;
    public boolean supports(Class clazz) {
        return Person.class.equals(clazz);
    }
    public void validate(Object obj, Errors e) {
        ValidationUtils.rejectIfEmpty(e, "name", "name.empty");
        Person p = (Person) obj;
        if (p.getAge() < 0) {
            e.rejectValue("age", "negativevalue");
        } else if (p.getAge() > 110) {
            e.rejectValue("age", "too.darn.old");
        }
    }
    public PersonValidator(Validator personValidator) {
        if (personValidator == null) {
            throw new IllegalArgumentException("The supplied [Validator] is " +
                "required and must not be null.");
        }
        if (!personValidator.supports(Person.class)) {
            throw new IllegalArgumentException("The supplied [Validator] must " +
                "support the validation of [Person] instances.");
        }
        this.personValidator = personValidator;
    }
}
```

## [BeanWrapper](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#beans-beans)

> JAVA Bean的定义

+ 具有默认无参数构造函数
+ 它遵循命名约定，成员变量具有get/set方法

BeanWrapper提供了可以获取javaBean中特定的或者所有的属性描述(描述符、类型、get/set方法)

```java
// javaBean的实例对象
Object getWrappedInstance();
// javaBean的class
Class<?> getWrappedClass();
// javaBean的所有属性描述
PropertyDescriptor[] getPropertyDescriptors();
// javaBean的指定属性描述 get/setMethod 修饰符 名称 ...
PropertyDescriptor getPropertyDescriptor(String propertyName);
```

API测试

```java
BeanWrapper company = new BeanWrapperImpl(new Company());
// 设置属性值 name属性值为 Some Company Inc.
company.setPropertyValue("name", "Some Company Inc.");
// 另一种设置方法
PropertyValue value = new PropertyValue("name", "Some Company Inc.");
company.setPropertyValue(value);
BeanWrapper jim = new BeanWrapperImpl(new Employee());
jim.setPropertyValue("name", "Jim Stravinsky");
company.setPropertyValue("managingDirector", jim.getWrappedInstance());
// 获取属性值  managingDirector 是一个对象 => 对象.属性
Float salary = (Float) company.getPropertyValue("managingDirector.salary");
```

## [Spring Type Conversion](https://docs.spring.io/spring-framework/docs/current/spring-framework-reference/core.html#core-convert)

转换到具体的类：Converter

```java
public interface Converter<S, T> {
	// S => T
    T convert(S source);
}
// 将对象转为字符串
final class ObjectToStringConverter implements Converter<Object, String> {
	public String convert(Object source) {
		return source.toString();
	}
}
```

转换到一个类型范围：ConverterFactory

```java
public interface ConverterFactory<S, R> {
	// 将S类型转为R类型，转换后的类型T是R的子类型
	<T extends R> Converter<S, T> getConverter(Class<T> targetType);
}
```

更加复杂的转换器：GenericConverter

```java
public interface GenericConverter {
	// 返回此转换器源类型和目标类型
    public Set<ConvertiblePair> getConvertibleTypes();
	// 具体的转换逻辑
    Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);
}
```

特定条件下的转换器：ConditionalGenericConverter

```java
public interface ConditionalConverter {
	// 匹配条件
    boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType);
}
public interface ConditionalGenericConverter extends GenericConverter, ConditionalConverter {
}
```

ConversionService用于在运行时执行类型转换逻辑

```java
public interface ConversionService {
	// 判断一个类型是否可以转换
    boolean canConvert(Class<?> sourceType, Class<?> targetType);
	// 具体的转换逻辑
    <T> T convert(Object source, Class<T> targetType);
	// 判断一个TypeDescriptor是否可以转换 转换 类、方法、字段
    boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType);
	// 转换TypeDescriptor
    Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType);
}
```

编程的方式使用转换器

```java
DefaultConversionService convertService = new DefaultConversionService();
List<Integer> input = ...
convertService.convert(input,
    TypeDescriptor.forObject(input), // List<Integer> type descriptor
    TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(String.class)));
```

## Configuring a Global Date and Time Format

> 使用**@DateTimeFormat**注解的时间字段，默认的转换标准为DateFormat.SHORT，`date:M/d/yy time: h:mm a`

使用自定义的日期格式

```java
@Configuration
public class AppConfig {
    @Bean
    public FormattingConversionService conversionService() {
        // 使用默认的服务类，但是修改它格式化后的格式
        DefaultFormattingConversionService conversionService = new DefaultFormattingConversionService(false);
        // Ensure @NumberFormat is still supported
        conversionService.addFormatterForFieldAnnotation(new NumberFormatAnnotationFormatterFactory());
        // 注册这个转换器
        DateFormatterRegistrar registrar = new DateFormatterRegistrar();
        registrar.setFormatter(new DateFormatter("yyyyMMdd"));
        registrar.registerFormatters(conversionService);

        return conversionService;
    }
}
```







