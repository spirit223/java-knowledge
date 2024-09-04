package cc.sika.netty.c1;

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
