package cc.sika.netty.loop;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * echo server
 *
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class EchoServer {
    private static final int PORT = 8088;

    public static void main(String[] args) {
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel nioSocketChannel) throws Exception {
                        nioSocketChannel.pipeline()
                                .addLast(new LoggingHandler(LogLevel.DEBUG))
//                                .addLast(new StringDecoder(StandardCharsets.UTF_8))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        log.info("receive msg: {}", msg);
                                        super.channelRead(ctx, msg);
                                    }
                                })
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                        ByteBuf response = ctx.alloc().buffer();
                                        if (msg instanceof ByteBuf) {
                                            log.debug("msg is ByteBuf");
                                            response.writeBytes((ByteBuf) msg);
                                        }
                                        else if (msg instanceof String) {
                                            log.debug("msg is String");
                                            response.writeBytes(msg.toString().getBytes(StandardCharsets.UTF_8));
                                        }
                                        ctx.writeAndFlush(response);
                                    }
                                });
                    }
                });
        serverBootstrap.bind(8088);
        log.info("server is running on port: {}!", PORT);
    }
}
