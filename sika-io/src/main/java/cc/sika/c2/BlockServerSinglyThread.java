package cc.sika.c2;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


/**
 * NIO 阻塞服务器示例
 * 在单线程中处理客户端请求
 * 多个客户端请求会被阻塞
 */
@Slf4j
public class BlockServerSinglyThread {
    public static void main(String[] args) throws IOException {

        List<SocketChannel> clients = new ArrayList<>();
        try (ServerSocketChannel ssc = ServerSocketChannel.open()) {
            ByteBuffer buffer = ByteBuffer.allocate(16);

            ssc.bind(new InetSocketAddress(8088));

            while (true) {
                log.debug("connecting...");
                SocketChannel clientChannel = ssc.accept();
                log.debug("accepted! {}", clientChannel);
                clients.add(clientChannel);
                for (SocketChannel client : clients) {
                    log.debug("before read ~~ {}", client);
                    client.read(buffer);
                    buffer.flip();
                    log.info("client info is: {}", StandardCharsets.UTF_8.decode(buffer));
                    buffer.clear();
                    log.debug("after clear *** {}", client);
                }
            }
        }
    }
}
