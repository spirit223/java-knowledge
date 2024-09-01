package cc.sika.blocking;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;


/**
 * NIO 服务器实例的演示客户端
 */
@Slf4j
public class Client {
    private static final InetSocketAddress SOCKET_ADDRESS = new InetSocketAddress("127.0.0.1", 8088);
    public static void main(String[] args) throws IOException {

        try (SocketChannel socketChannel = SocketChannel.open()) {
            socketChannel.connect(SOCKET_ADDRESS);
            log.info("connected server!");

            // simulated transmission data
            for (int i = 1; i <= 10; i++) {
                // send
                String clientMessage = String.format("message%d\n", i);
                socketChannel.write(StandardCharsets.UTF_8.encode(clientMessage));
                log.debug("send message: {}", clientMessage);

                // accept ack message
                ByteBuffer ackBuffer = ByteBuffer.allocate(1024);
                int readLength = socketChannel.read(ackBuffer);
                if (readLength > 0) {
                    ackBuffer.flip();
                    String response = StandardCharsets.UTF_8.decode(ackBuffer).toString();
                    log.info("response: {}", response);
                }
                ackBuffer.clear();
                // sleep
                log.info("sleep...");
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
