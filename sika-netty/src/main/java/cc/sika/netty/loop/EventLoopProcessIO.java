package cc.sika.netty.loop;

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
