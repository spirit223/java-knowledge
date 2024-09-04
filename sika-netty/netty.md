# 一. C/S

## 1.1 服务器创建

Netty创建服务器使用 `ServerBootstrap` 类来组装,

`ServerBootstrap` 是一个服务器启动类, 可以组装服务器所需的所有组件, 创建启动类后要给定一个事件循环组来处理服务器连接事件以及处理连接的通道类型, 最重要的是指定读写事件发生时应该做什么操作, 最后绑定特定端口即可

- 给定事件循环组通过 `.group(EventLoopGroup)` 完成

- 指定处理连接通道类型通过 `.channel(class)` 完成

- 处理读写事件使用 `childHandler(ChannelHandler)` 完成

  channelHandler通常使用抽象类 `ChannelInitializer<T>`实现initChannel方法完成, 泛型为连接成功后的通道类型, `NioServerSocketChannel`对应的就是`NioSocketChannel`

  > childHandler 不能直接处理读写事件, 需要通过netty的channel中的pipeline来完成, 一个childHandler可以拥有多个pipeline对一条消息进行多次处理
  >
  > 在ChannelInitializer类的initChannel方法可以得到channel, 调用channel.pipeline()可以获得管道并添加或删除

下面是一个简单的服务器实例, 服务器接收一个消息后通过UTF-8编码然后由第二个处理器完成消息的日志推送

```java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class NettyBasicServer {
    public static void main(String[] args) {
        // server launch, assemble netty module
        new ServerBootstrap()
        // group include boss and worker(selector, thread), boss process accept, worker process read/write
        .group(new NioEventLoopGroup())
        // using server socket channel as boss, it is nio mode, but not jdk nio, it belongs to netty
        .channel(NioServerSocketChannel.class)
        // appoint what should worker do
        // this initializer can create channel, in initializer add some channel to process read/write
            .childHandler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel channel) throws Exception {
                // stringDecoder will change ByteBuf to java.lang.String (ByteBuf belongs to netty)
                channel.pipeline().addLast(new StringDecoder());
                // add consume handler to handle read with ChannelInBoundHandler
                channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        log.info("msg is [{}]", msg);
                    }
                });
            }
        })
        .bind(8088);
    }
}
```

## 1.2 客户端创建

实际上, Netty创建的客户端不需要使用Netty实现的客户端来连接, 任何基于传输层协议的连接都可以完成(TCP/UDP).

如果要使用Netty创建客户端, 代码与服务器创建类似, 只需要将服务器类型转为客户端类型即可

1. 使用客户端启动类创建启动器

   > 客户端启动类为Bootstrap, JDK中也有一个类加载器名为Bootstrap

2. 指定事件循环组

3. 指定启动类中的通道类型

4. 添加处理器

   > 这里的处理器添加方法使用的是handle而不是服务器的childHandle

```java
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;

/**
 * @author spirit
 * @since 2024-09
 */
public class NettyBasicClient {
    public static void main(String[] args) throws InterruptedException{
        // client launcher
        new Bootstrap()
        .group(new NioEventLoopGroup())
        // appoint worker class
        .channel(NioSocketChannel.class)
        // add handler, it will invoke after connecting
        .handler(new ChannelInitializer<NioSocketChannel>() {
            @Override
            protected void initChannel(NioSocketChannel ch) throws Exception {
                // because server has string decoder, so here need to add string encoder
                // to make sure message coding is the same
                ch.pipeline().addLast(new StringEncoder());
            }
        })
        .connect(new InetSocketAddress("127.0.0.1", 8088))
        // blocking until connection was established
        .sync()
        // get connection channel to send message to server
        .channel().writeAndFlush("hello, world!");
    }
}
```

# 二. 组件

## 2.1 EventLoop

eventLoop是Netty中真正的事件执行者, 是一个单线程的执行器, 实现了JUC的Executor接口并提供一个run方法具备线程运行能力, 并且在内部持有一个selector来处理执行的任务. 

EventLoop 一定会绑定到一个特定的线程

EventLoop接口继承netty的`OrderedEventExecutor`接口, `OrderedEventExecutor`接口继承JUC包中的 `ScheduledExecutorService`接口, 使得 EventLoop 具备JUC提供的多线程定时任务执行能力 与 netty 提供的判断线程与 EventLoop 关系的能力.

- isEventLoop(Thread thread) 判断线程是否属于 EventLoop

- parent 可以获取到所属的 EventLoopGroup

EventLoop使用主要有两类:

- NioEventLoop: 能够处理 io 事件, 定时任务与普通任务
- DefaultEventLoop: 没有处理io事件的能力

NioEventLoop的构造器是包私有的, 通常在使用EventLoop都是通过Group来完成

即使用 `EventLoopGroup`. EventLoop 既可以充当处理连接的BOSS也可以充当处理读写的Worker, 在创建EventLoopGroup时如果不指定数量, 会创建出多个EventLoop, 并让不同的EventLoop处理不同的事件.

不指定数量时, 会按照机器的核心数来决定, 以 `NioEventLoopGroup` 为例

```java
public class NioEventLoopGroup extends MultithreadEventLoopGroup {
    public NioEventLoopGroup() {
        // 空参数时传递0, 在父类构造时进行处理
        this(0);
    }
    public NioEventLoopGroup(int nThreads, Executor executor, final SelectorProvider selectorProvider,
                             final SelectStrategyFactory selectStrategyFactory) {
        super(nThreads, executor, selectorProvider, selectStrategyFactory, RejectedExecutionHandlers.reject());
    }
}

public abstract class MultithreadEventLoopGroup extends MultithreadEventExecutorGroup implements EventLoopGroup {
    // 默认事件循环线程数
    private static final int DEFAULT_EVENT_LOOP_THREADS;

    static {
        // 至少一个, 默认为机器CPU核心数*2
        DEFAULT_EVENT_LOOP_THREADS = Math.max(1, SystemPropertyUtil.getInt(
                "io.netty.eventLoopThreads", NettyRuntime.availableProcessors() * 2));

        if (logger.isDebugEnabled()) {
            logger.debug("-Dio.netty.eventLoopThreads: {}", DEFAULT_EVENT_LOOP_THREADS);
        }
    }
    
    protected MultithreadEventLoopGroup(int nThreads, Executor executor, Object... args) {
        super(nThreads == 0 ? DEFAULT_EVENT_LOOP_THREADS : nThreads, executor, args);
    }
}
```

空参数创建的`EventLoopGroup`线程数为 **机器核心数*2**

下面是 EventLoop 的处理普通任务和定时任务的方式

```java
// show how to use EventLoop handle task and schedule task
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * using event loop to process default task and schedule task
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class EventLoopRunTask {
    public static void main(String[] args) {
        // create EventLoop, use EventLoop need by group.
        // NioEventLoopGroup can process io event,schedule task, default task
        // DefaultEventLoopGroup can not process io event
        EventLoopGroup group = new NioEventLoopGroup(2);
        // EventLoopGroup group = new NioEventLoopGroup();
        // EventLoopGroup group2 = new DefaultEventLoopGroup();

        // get event loop by next() and process default task
        log.info("first EventLoop: [{}]", group.next());
        log.info("second EventLoop: [{}]", group.next());
        log.info("third EventLoop: [{}]", group.next());
        log.info("fourth EventLoop: [{}]", group.next());

        // invoke default task by submit() or execute()
        EventLoop eventLoop = group.next();
        eventLoop.submit(() -> {
            log.info("submit by event loop (running default task)");
        });

        // eventLoop interface extended ScheduledExecutorService, so it can invoke schedule task
        // invoke schedule task with scheduleAtFixedRate() or scheduleAtFixedDelay()
        // delay one seconds log row message
        group.next().scheduleAtFixedRate(()->{
            log.info("schedule by event loop (running schedule task)");
        }, 0, 1, TimeUnit.SECONDS);

        // thread of event loop run task not the same as main
        log.info("main thread");
    }
}
```

使用 NioEventLoop 处理 IO 的方式与服务器创建的方式类似

```java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * using nioEventLoop to process io event as a server
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class EventLoopProcessIO {
    public static void main(String[] args) {
        new ServerBootstrap()
                .group(new NioEventLoopGroup(2))
                .channel(NioServerSocketChannel.class)
                // every time client connected, server will make one channel binding in eventLoop.
                // one client for one channel, channel binding to eventLoop
                // event loop run with singly thread can make sure handle io event is concurrency safe.
                // if the number of client connection more than the number of eventLoopGroup thread
                // Multiplexing use one event loop(one thread) handle multiple connection(channel)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                // if never process msg, it will be a ByteBuf.
                                // add String Decoder before this pipeline, msg will be a java.lang.String
                                ByteBuf buf = (ByteBuf) msg;
                                log.info("buf is [{}]", buf.toString(StandardCharsets.UTF_8));
                                log.info("channel is [{}]", ctx.channel());
                            }
                        });
                    }
                }).bind(8088);

        log.info("server is running");
    }
}
```

值得注意的是, 创建服务器时会使用 NioEventLoop 完成连接请求, 连接建立后会有一个新的 channel 为 NioSocketChannel, `NioSocketChannel` 绑定到由

```java
.group(new NioEventLoopGroup(2))
```

创建出来的NioEventLoopGroup中的NioEventLoop. 

每个NioEventLoop也会绑定到一个特定的线程来处理, netty会尝试使用不同的NioEventLoop来处理读写达到负载均衡的目的, 但是在连接数超过给定的Group线程数量时, 将有多个 NioSocketChannel 被绑定到 同一个 NioEventLoop 上, 类似于一个线程处理多个channel.

这样处理的好处是, 一个线程单独处理一个连接的IO, 不会产生竞态条件导致多线程安全问题, 也无需为资源处理进行锁控制, 减少上下文切换等耗时操作同时避免死锁的可能.

## 2.2 Channel

## 2.3 Future & Promise

## 2.4 Handler & Pipeline

## 2.5 ByteBuf