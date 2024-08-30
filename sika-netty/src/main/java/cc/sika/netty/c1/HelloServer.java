package cc.sika.netty.c1;

import io.micrometer.common.lang.NonNull;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;

import java.nio.charset.StandardCharsets;

public class HelloServer {
    public static void main(String[] args) {
        /* 服务器启动器 */
        new ServerBootstrap()
            .group(new NioEventLoopGroup())
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(@NonNull NioSocketChannel nioSocketChannel) {
                    nioSocketChannel.pipeline()
                        .addLast(new StringDecoder(StandardCharsets.UTF_8))
                        .addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                System.out.println("msg = " + msg);
                            }
                        });
                }
            }).bind(8080);
    }
}
