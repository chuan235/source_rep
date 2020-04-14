### docker

docker利用Linux中核心的资源分离机制：cgroups(control groups) 和Linux核心namespace，来创建独立的容器

> 这可以在单一Linux实体下运作，避免引导一个虚拟机造成的额外负担。

+ Linux核心对名字空间的支持完全隔离了工作环境中应用程序的视野，包括行程树、网络、用户ID与挂载文件系统
+ 核心的cgroup提供资源隔离，包括CPU、存储器、block I/O与网络。

> 从0.9版本起，Dockers在使用抽象虚拟是经由libvirt的LXC与systemd - nspawn提供界面的基础上,开始包括libcontainer库做为以自己的方式开始直接使用由Linux核心提供的虚拟化的设施，

CI Tools

+ bamboo
+ buildot
+ gump
+ travis ci
+ jenkins

### Zookeeper的日志管理

+ 事务日志
+ 快照日志
+ log4j日志

> 日志路径

+ 在zookeeper的配置文件中有一个时dataDir的配置项，这个配置项就是配置zookeeper快照和事务日志的存储路径
+ 如果需要将事务日志和快照日志分开，可以使用dataLogDir和dataLog这个两个配置项。dataLogDir是事务日志的存放路径，dataLog是存放快照日志的目录
+ log4j的路径配置，在conf/目录下的log4j.properties文件中有一个zookeeper.log.dir的配置项，这里就配置服务端log4j日志存放的位置

> 日志的作用

+ 事务日志：zookeeper系统在进行数据操作之前，首先会开启一个事务id，并将本次事务id，操作类型等记录到一个文件中，只有记录成功之后，zookeeper才会去内存中更新数据，最后响应客户端。
+ 快照日志：zookeeper的数据在内存中是以始终DataTree的数据结构存储的，而快照就是每隔一段时间就会把整个dataTree的数据序列化到磁盘中
+ log4j日志就是记录zookeeper集群服务器的输出日志信息

> 日志清理

在zookeeper的3.4之后，提供了一种自动清理事务日志和快照日志的功能

```properties
# The number of snapshots to retain in dataDir 自动清理日志时保留的快照数
autopurge.snapRetainCount=3
# Purge task interval in hours 自动清理数据的间隔时间，单位小时
# Set to "0" to disable auto purge feature 0表示不开启自动清理日志
autopurge.purgeInterval=1
```

### NIO简介

### 概念

> NIO主要由三部分组成：Channel、Buffer、Selector

+ 工作原理：有一个专门的线程处理所有IO事件并负责分发，事件发生的时候自然去触发事件，而不是去同步的监听事件

通道(channel)：表示对支持IO操作的实体的一个连接。这个通道打开之后就可以执行读取和写入操作。与传统的流操作相比，通道操作的是buffer而不是数组，使用更加灵活。通道的引入提升了I/O操作的灵活性和性能。

缓冲区(Buffer)：NIO中引入的Buffer抽象类，可以很灵活的根据这个抽象类自定义合适的缓冲区结构。

```java
private int position = 0;// buffer当前读写位置
private int limit;// 可用的读写范围
private int capacity;// 容量限制
//buffer中的写或者读操作都存在两种形式，分为相对和绝对两种方式，相对就是相对与position的位置，而绝对就需要指定起始的位置
public abstract ByteBuffer put(byte b);// 相对写
public abstract ByteBuffer put(int index, byte b);// 绝对写
```

在相对读或者相对写时，为了防止position的值发生变化，建议在读之前调用buffer.clear方法，在写之前调用buffer.flip方法

```java
// 读
buf.clear();
in.read(buf);
// 写
buf.flip();
out.write(buf);
```

### FileChannel

对文件操作时，文件通道FileChannel提供了与其它通道之间高效传输数据的能力，相比于传统的基于流和字节数组作为缓冲区的做法，FileChannel更加简单和快速

```java
// url网页上的内容保存到本地文件中
URL url = new URL("http://www.baidu.com");
ReadableByteChannel readChannel = Channels.newChannel(url.openStream());
// 构建一个FileChannel
FileOutputStream output = new FileOutputStream("baidu.html");
FileChannel channel = output.getChannel();
// 下载
channel.transferFrom(readChannel, 0, Integer.MAX_VALUE);
```

### SocketChannel

> socketChannel相比与传统的socket增加了对非阻塞I/O和多路复用I/O的支持
>
> 传统的I/O操作是阻塞式的，在进行I/O操作的时候线程会处于阻塞状态等待操作完成

在NIO中引入了对非阻塞I/O的支持，不过仅限于socket的I/O操作，所有继承自SelectableChannel的通道类都可以通过configureBlocking方法来设置是否采用非阻塞模式。在非阻塞模式下，程序可以在适当的时候查询是否有数据可供读取，一般是通过定期的轮询来实现的。

### NIO操作类型

| NIO    | 通道类型            | OP_ACCEPT | OP_CONNECT | OP_WRITE | OP_READ |
| ------ | ------------------- | --------- | ---------- | -------- | ------- |
| 客户端 | SocketChannel       |           | Y          | Y        | Y       |
| 服务端 | ServerSocketChannel | Y         |            |          |         |
| 服务端 | SocketChannel       |           |            | Y        | Y       |

在客户端的SocketChannel支持三个操作，服务端的ServerSocketChannel只支持OP_ACCEPT操作。

+ SocketChannel是由ServerSocketChannel的accept方法产生的SocketChannel，支持OP_WRITE和OP_READ

> 操作类型的就绪条件

+ 当接收到一个客户端的连接请求时，ON_ACCEPT操作就绪
+ 当客户端调用SocketChannel.connect()时，OP_CONNECT操作就绪
+ 当系统中的写Buffer缓冲区中空闲的空间时，OP_WRITE操作就绪
+ 当系统中的读Buffer缓冲区中有数据可读时，OP_READ操作就绪

[NIO编程示例](https://www.jianshu.com/p/ad16183b88cf)






