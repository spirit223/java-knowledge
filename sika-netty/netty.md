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

channel 的主要作用:

- close() 用来关闭channel
- closeFuture() 处理channel的关闭
  - sync 同步等待channel关闭
  - addListener 异步等待channel关闭
- pipeline 添加处理器
- write 将数据写入channel(不一定发送)
- writeAndFlush 写入数据并发送(刷出)

回顾客户端代码

```java
new Bootstrap()
.group(new NioEventLoopGroup())
.channel(NioSocketChannel.class)
.handler(new ChannelInitializer<NioSocketChannel>() {
    @Override
    protected void initChannel(NioSocketChannel ch) throws Exception {
        ch.pipeline().addLast(new StringEncoder());
    }
})
.connect(new InetSocketAddress("127.0.0.1", 8088))
.sync()
.channel().writeAndFlush("hello, world!");
```

在客户端创建, 连接服务器后调用了 `sync` 方法, 这个方法将会阻塞, 知道实际连接成功, 如果不调用该方法, 在connect方法执行连接请求后就会走到 `channel`方法直接获取到一个channel, 由于网络连接建立需要时间, 此时获取到的channel是一个未连接状态的channel, 直接执行 writeAndFlush 会导致数据丢失, 服务器未能正常接收到数据

> connect方法返回的是一个ChannelFuture接口, sync 和 channel方法都属于该接口

sync是一个阻塞方法, 如果不想客户端阻塞, 可以采用 ChannelFuture 接口的 addListener 方法, 为ChannelFuture添加监听器, 类似回调函数, 在特定的时机执行监听器内容

```java
ChannelFuture channelFuture = new Bootstrap()
    .group(new NioEventLoopGroup())
    .channel(NioSocketChannel.class)
    .handler(new ChannelInitializer<NioSocketChannel>() {
        @Override
        protected void initChannel(NioSocketChannel ch) throws Exception {
            ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));
        }
    })
    // connect 返回 ChannelFuture 接口对象
    .connect(new InetSocketAddress("localhost", 8088));

// 为 ChannelFuture 添加监听器, 在连接完成后由nio线程执行回调内容
channelFuture.addListener(new ChannelFutureListener() {
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        Channel channel = future.channel();
        channel.writeAndFlush("Hello World");
    }
});
```

sync方法是将整个过程进行阻塞, 将异步变为同步

监听器方法是保持异步性, 在特定的时机执行回调, 回调的内容将由另外的线程即 nio 线程进行处理

### channel关闭处理

现在设计一个客户端, 用于接收用户的输入, 并且在用户每次输入完成按下回车后都可以将数据发送到服务器, 并在按下 'q' 时退出

```java
@Slf4j
public class SendIOClient {
    public static void main(String[] args) {
        // create client, and get channel future by connect method.
        NioEventLoopGroup group = new NioEventLoopGroup();
        ChannelFuture clientConnectFuture = new Bootstrap()
                .group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));
                    }
                }).connect(new InetSocketAddress("localhost", 8088));

        // get channel by client channel future.
        Channel channel = clientConnectFuture.channel();
        // after connecting, create new thread receive user input
        // and put input message to server util user input 'q'.
        // add listener for client connect future, this listener will listen connect event
        // (after connect method connecting successful)
        clientConnectFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("connect success, run scanner in input thread");
                group.next().submit(()->{
                    while (true){
                        Scanner input = new Scanner(System.in);
                        String line = input.nextLine();
                        if ("q".equals(line)) {
                            channel.close();
                            break;
                        }
                        channel.writeAndFlush(line);
                    }
                });
            }
        });
    }
}
```

在上面的实现中, 通过 connect 方法连接成功得到一个 `ChannelFuture`, 并且绑定了一个监听器, 每次连接成功时会在 `EventLoopGroup` 中取出一个执行器, 类似一个线程来接收用户输入, 当用户输入的内容是 'q' 时, 将该连接的通道关闭.

但是通道关闭后整个程序并不会停下, 因为在创建  NIOEventLoopGroup 的时候同时创建出来了多个线程, 连接通道关闭了但是仍然后很多线程在执行, 需要连同整个eventLoopGroup一起关闭才可以.

如果在if语句块中执行group关闭操作会有一个问题, 执行接收用户输入的是group中的一个执行器, 而close方法是一个异步方法, 如果想在通道关闭完成后再执行相应的善后工作并不能保证都是在close完成之后再执行的, 例如打印一个程序关闭的日志, 这个日志可能出现在close方法执行完成之前

```
22:53:13.076 [nioEventLoopGroup-2-2] INFO  cc.sika.netty.channel.SendIOClient - close channel and exit
22:53:13.076 [nioEventLoopGroup-2-1] DEBUG io.netty.handler.logging.LoggingHandler - [id: 0x5aa15679, L:/127.0.0.1:6072 - R:localhost/127.0.0.1:8088] CLOSE
```

想要真正完成善后工作, 需要通过该连接成功的channel对象的 `closeFuture` 方法得到一个 `ChannelFuture` 接口并将代码阻塞为同步, 等待成功关闭后再执行善后工作

```java
// closeFuture method mean gives you future, you can deal shutdown task after channel being closed,
ChannelFuture channelFuture = channel.closeFuture();
try {
	// blocking main thread by closeFuture's sync method until channel closes success
	channelFuture.sync();
	log.info("after closing, now process shutdown task...");
} catch (InterruptedException e) {
	throw new RuntimeException(e);
}
```

这种方式将阻塞整个main线程执行, 不符合nio思想

netty提供了监听器回调的方式, 避免阻塞代码, 同样是在连接成功的channel对象调用  `closeFuture` 得到 `ChannelFuture` 并为该 future 添加监听器

```java
// note: just close channel, client has not been shutdown. because NioEventLoopGroup has other thread.
// NioEventLoopGroup's shutdownGracefully method can
// make all the thread in NioEventLoopGroup be shutdown
// after thread task completing
channel.closeFuture().addListener(new ChannelFutureListener() {
    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        log.info("after closing, now process shutdown task...");
        group.shutdownGracefully();
    }
});
```

该监听器的`operationComplete`方法逻辑会在通道真正关闭之后被执行.

## 2.3 Future & Promise

### JUC--Future

JUC包提供一个 Future 接口, 用于获取线程的任务执行状态, 主要与线程池和 callback 接口搭配使用.

```java
import java.util.concurrent.*;

/**
 * show how to use java.util.concurrent.Future with thread pool
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class JDKFuture {
    public static void main(String[] args) {
        // create thread pool
        ExecutorService pool = Executors.newFixedThreadPool(2);

        log.info("submit task");
        Future<Integer> future = pool.submit(new Callable<Integer>() {
            public Integer call() throws Exception {
                Thread.sleep(2000);
                return 1;
            }
        });
        log.info("waiting result");
        // future's get method is blocking, it will block until task complete
        // so here need to wait for task complete (2000ms)
        try {
            log.info("result is {}", future.get());
        } catch (InterruptedException|ExecutionException e) {
            throw new RuntimeException(e);
        }
        pool.shutdown();
    }
}
```

future通过get方法, 阻塞等待直到任务完成返回计算结果

### Netty--Future

netty也提供一个Future接口, 扩展 JUC 的Future接口, 添加监听器相关功能方法, 判断成功失败相关方法, 等待以及将代码同步化相关方法以及获取结果的getNow()

```java
package io.netty.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

public interface Future<V> extends java.util.concurrent.Future<V> {
    boolean isSuccess();
    boolean isCancellable();
    Throwable cause();
    Future<V> addListener(GenericFutureListener<? extends Future<? super V>> listener);
    Future<V> addListeners(GenericFutureListener<? extends Future<? super V>>... listeners);
    Future<V> removeListener(GenericFutureListener<? extends Future<? super V>> listener);
    Future<V> removeListeners(GenericFutureListener<? extends Future<? super V>>... listeners);
    Future<V> sync() throws InterruptedException;
    Future<V> syncUninterruptibly();
    Future<V> await() throws InterruptedException;
    Future<V> awaitUninterruptibly();
    boolean await(long timeout, TimeUnit unit) throws InterruptedException;
    boolean await(long timeoutMillis) throws InterruptedException;
    boolean awaitUninterruptibly(long timeout, TimeUnit unit);
    boolean awaitUninterruptibly(long timeoutMillis);
    /**
     * Return the result without blocking. If the future is not done yet this will return {@code null}.
     *
     * As it is possible that a {@code null} value is used to mark the future as successful you also need to check
     * if the future is really done with {@link #isDone()} and not rely on the returned {@code null} value.
     */
    V getNow();
    @Override
    boolean cancel(boolean mayInterruptIfRunning);
}
```

因为netty提供的future接口增加了监听器相关功能, 所以可以使用监听器避免线程阻塞问题

```java
import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

/**
 * show how to use io.netty.util.concurrent.Future with EventLoopGroup(as thread pool)
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class NettyFuture {
    public static void main(String[] args) {
        // create event loop group(like thread pool)
        EventLoopGroup group = new NioEventLoopGroup();
        EventLoop eventLoop = group.next();

        log.info("submit task");
        Future<Integer> nettyFuture = eventLoop.submit(() -> {
            Thread.sleep(1000);
            return 1;
        });
        log.info("waiting for result");

        // netty's future is blocking
        // it has to wait for task completing and return result
        /*try {
            log.info("result is {}", nettyFuture.get());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }*/

        // use listener and future's getNow method to obtain the task result asynchronously
        nettyFuture.addListener(new GenericFutureListener<Future<? super Integer>>() {
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                log.info("result is {}", future.getNow());
            }
        });

        // shutdown all thread in group
        group.shutdownGracefully();
    }
}
```

### Netty--Promise

Netty设计Promise继承自io.netty.util.concurrent.Future, 添加设置成功或失败的标记能力

```java
package io.netty.util.concurrent;
public interface Promise<V> extends Future<V> {
    Promise<V> setSuccess(V result);
    boolean trySuccess(V result);
    Promise<V> setFailure(Throwable cause);
    boolean tryFailure(Throwable cause);
    boolean setUncancellable();
}
```

通过设置成功和失败可以结合监听器发挥更加灵活的能力

```java
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;


/**
 * show how to use io.netty.util.concurrent.Promise.
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class NettyPromise {
    public static void main(String[] args) {
        // create pool
        NioEventLoopGroup loopGroup = new NioEventLoopGroup();
        // get executor (thread)
        EventLoop eventLoop = loopGroup.next();
        // promise need one executor to run task
        DefaultPromise<Integer> promise = new DefaultPromise<>(eventLoop);

        // use another thread to simulate task, after completing it need to put result to promise
        loopGroup.next().submit(()->{
            log.info("starting task");
            try {
                Thread.sleep(1000);
                int i = 1/0;
                promise.setSuccess(23);
            } catch (Exception e) {
                // put error message
                promise.setFailure(e);
            }
        });

        log.info("waiting result...");
        // the get method is from JDK's Future interface, so it is blocking
//        try {
//            log.info("result is {}", promise.get());
//        } catch (InterruptedException | ExecutionException e) {
//            throw new RuntimeException(e);
//        }

        // Promise interface extend netty's Future, so it has addListener method
        // use listener can avoid synchronized
        promise.addListener(new GenericFutureListener<Future<? super Integer>>() {
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                if (future.isSuccess()) {
                    log.info("future.get() is {}", future.get());
                    log.info("promise.get() is {}", promise.get());
                }
                else {
                    log.error("promise.get() failed");
                    log.error("failure cause", future.cause());
                }

            }
        });

        loopGroup.shutdownGracefully();
    }
}
```

## 2.4 Handler & Pipeline

## 2.5 ByteBuf