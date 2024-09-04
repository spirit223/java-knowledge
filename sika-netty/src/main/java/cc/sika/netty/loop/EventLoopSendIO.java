package cc.sika.netty.loop;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * using nioEventLoop connect to server and send message
 * @author spirit
 * @since 2024-09
 */
public class EventLoopSendIO {
    public static void main(String[] args) throws InterruptedException {
        Channel clientChanel = new Bootstrap()
                .group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new StringEncoder(StandardCharsets.UTF_8));
                    }
                })
                .connect(new InetSocketAddress("localhost", 8088))
                .sync()
                .channel();

        // add break point at here, make sure point hang up as 'thread' not 'all'
        System.out.println("client unlined");
        // using debug to send message
        // clientChanel.writeAndFlush("some message");
    }
}
