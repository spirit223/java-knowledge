package cc.sika.netty.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @author spirit
 * @since 2024-09
 */
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
                group.next().submit(() -> {
                    while (true) {
                        Scanner input = new Scanner(System.in);
                        String line = input.nextLine();
                        if ("q".equals(line)) {
                            channel.close();
                            break;
                        }
                        channel.writeAndFlush(line);
                    }
                });
//                new Thread(()->{
//                    while (true){
//                        Scanner input = new Scanner(System.in);
//                        String line = input.nextLine();
//                        if ("q".equals(line)) {
//                            channel.close();
//                            break;
//                        }
//                        channel.writeAndFlush(line);
//                    }
//                }, "input").start();
            }
        });
        // closeFuture method mean gives you future, you can deal shutdown task after channel being closed,
//        ChannelFuture channelFuture = channel.closeFuture();
//        try {
//            // blocking main thread by closeFuture's sync method until channel closes success
//            channelFuture.sync();
//            log.info("after closing, now process shutdown task...");
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        // or you can deal shutdown task by listener
        // deal shutdown task by listener, it will be invoked in nio thread.
        // deal shutdown task by close future and sync, shutdown task will be invoked in main thread
//        channel.closeFuture().addListener(new ChannelFutureListener() {
//            @Override
//            public void operationComplete(ChannelFuture future) throws Exception {
//                log.info("after closing, now process shutdown task...");
//            }
//        });

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
    }
}
