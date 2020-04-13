### quick start

1、demo结构，使用api发布和消费服务

```tex
api
 -- Interface GreetingService
provider
 -- GreetingProvider
consumer
 -- ConsumerMain
```

2、pom文件

```xml
<properties>
    <dubbo.version>2.7.1</dubbo.version>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
</properties>
<dependencies>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo</artifactId>
        <version>${dubbo.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.dubbo</groupId>
        <artifactId>dubbo-dependencies-zookeeper</artifactId>
        <version>${dubbo.version}</version>
        <type>pom</type>
    </dependency>
</dependencies>
```

3、接口以及实现类

```java
public interface GreetingService {
    String sayHi(String name);
}
public class GreetingProvider implements GreetingService {
    @Override
    public String sayHi(String name) {
        return "hi! "+name;
    }
}
```

4、在GreetingProvider的main方法中发布服务

```java
public static final String zookeeperHost = System.getProperty("zookeeper.address", "192.168.222.129");
public static void main(String[] args) throws InterruptedException {
    ServiceConfig<GreetingService> service = new ServiceConfig<>();
    service.setApplication(new ApplicationConfig("first-dubbo-provider"));
    service.setRegistry(new RegistryConfig("zookeeper://" + zookeeperHost + ":2181"));
    service.setInterface(GreetingService.class);
    service.setRef(new GreetingProvider());
    service.export();
    System.out.println("dubbo service started");
    new CountDownLatch(1).await();
}
```

5、ConsumerMain的main方法中消费服务

```java
public static void main(String[] args) throws IOException {
    ReferenceConfig<GreetingService> reference = new ReferenceConfig<>();
    reference.setApplication(new ApplicationConfig("first-dubbo-consumer"));
    reference.setRegistry(new RegistryConfig("zookeeper://" + ProviderMain.zookeeperHost + ":2181"));
    reference.setInterface(GreetingService.class);
    GreetingService service = reference.get();
    String message = service.sayHi("dubbo");
    System.out.println(message);
    System.in.read();
}
```

启动过程中的问题：

consumer启动的时候出现 qos-server can not bind localhost:22222, dubbo version: 2.6.4, dubbo version: 2.7.0, current host: 169.254.94.86 java.net.BindException: Address already in use: bind    **单机启动服务提供者和服务消费者时存在端口占用的异常**

Qos(Quality of Service)是dubbo的在线运维命令，可以对服务进行动态的配置、控制及查询，新版本的telnet端口与dubbo协议的端口是不同的端口，默认为22222，可以通过配置文件dubbo.properties修改。telnet 模块现在同时支持 http 协议和 telnet 协议，方便各种情况的使用。

配置参数如下：

| 参数               | 说明             | 默认值 |
| ------------------ | ---------------- | ------ |
| qosEnable          | 是否启动Qos      | true   |
| qosPort            | Qos绑定的端口    | 22222  |
| qosAcceptForeignIp | 是否允许远程访问 | false  |

这些参数的配置和dubbo其他参数的配置方式相同，存在4种方法来配置这些参数

+ JVM系统属性
+ dubbo.properties
+ xml方式
+ springboot自动装配的方式

JVM系统属性 > dubbo.properties > XML/Spring-boot自动装配方式。

使用 添加JVM属性的方式重新启动服务

privoder: -Djava.net.preferIPv4Stack=true -Ddubbo.application.qos.enable=true -Ddubbo.application.qos.port=33333  -Ddubbo.application.qos.accept.foreign.ip=false

consumer: -Djava.net.preferIPv4Stack=true

启动成功

### 安装telnet 连接Qos

```shell
rpm -qa | grep telnet
yum list | grep telnet
# 安装
yum install -y telnet-server.x86_64
yum install -y telnet.x86_64
```

使用telnet 连接consumer

```shell
telnet 169.254.94.86 22222
[root@docker ~]# telnet 169.254.94.86 22222
Trying 169.254.94.86...
Connected to 169.254.94.86.
Escape character is '^]'.
   ___   __  __ ___   ___   ____     
  / _ \ / / / // _ ) / _ ) / __ \  
 / // // /_/ // _  |/ _  |/ /_/ /    
/____/ \____//____//____/ \____/   
dubbo>ls
Foreign Ip Not Permitted.
Connection closed by foreign host.
[root@docker ~]# 
```

连接不成功，因为配置了`dubbo.application.qos.accept.foreign.ip=false`

修改上面的值的true，再次尝试

```shell
telnet 169.254.94.86 22222
[root@docker ~]# telnet 169.254.94.86 22222
Trying 169.254.94.86...
Connected to 169.254.94.86.
Escape character is '^]'.
   ___   __  __ ___   ___   ____     
  / _ \ / / / // _ ) / _ ) / __ \  
 / // // /_/ // _  |/ _  |/ /_/ /    
/____/ \____//____//____/ \____/   
dubbo>ls
PROVIDER:
top.gmfcj.api.DemoService


dubbo>
```

```properties
 status [-l]                      - Show status.
 shutdown [-t <milliseconds>]     - Shutdown Dubbo Application.
 pwd                              - Print working default service.
 trace [service] [method] [times] - Trace the service.
 help [command]                   - Show help.
 exit                             - Exit the telnet.
 invoke [service.]method(args)    - Invoke the service method.
 clear [lines]                    - Clear screen.
 count [service] [method] [times] - Count the service.
 ls [-l] [service]                - List services and methods.
 log level                        - Change log level or show log 
 select [index]                   - Select the index of the method you want to invoke.
 ps [-l] [port]                   - Print server ports and connections.
 cd [service]                     - Change default service.
```















