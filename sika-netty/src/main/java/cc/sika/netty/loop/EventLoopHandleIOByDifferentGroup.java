package cc.sika.netty.loop;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * using multiple EventLoopGroup handle client connection.
 * <ul>
 *     <li>default EventLoop can not handle io event, let it to handle task that time-consuming</li>
 *     <li>NioEventLoop handle io event</li>
 * </ul>
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class EventLoopHandleIOByDifferentGroup {
    public static void main(String[] args) {
        EventLoopGroup defaultEventLoopGroup = new DefaultEventLoopGroup();

        new ServerBootstrap()
                // make first parameter as boss to handle accept
                // make second parameter as worker to handle read/write
                // boss just handle accept, do not bind thread to client, so one thread is enough
                .group(new NioEventLoopGroup(), new NioEventLoopGroup())
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        // using NioEventLoopGroup handle in-bound msg first
                        ch.pipeline().addLast("nio-event-handle", new ChannelInboundHandlerAdapter(){
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                log.info("in nio-event-handle buf is [{}]", buf.toString(StandardCharsets.UTF_8));
                                // transfer msg to next handle, next handle is defaultEventLoopGroup
                                // if thread of next handle is difference to current group thread, you have to invoke this method transfer it.
                                ctx.fireChannelRead(msg);
                            }
                        })
                        .addLast(defaultEventLoopGroup, "default-event-handle", new ChannelInboundHandlerAdapter() {
                            // as time-consuming task
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                log.info("in default-event-handle buf is [{}]", buf.toString(StandardCharsets.UTF_8));
                            }
                        });
                        // you will see twice log at one time received, and they were difference thread
                    }
                })
                .bind(8088);
    }
}
