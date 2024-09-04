package cc.sika.netty.c1;

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
