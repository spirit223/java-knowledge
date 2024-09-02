package cc.sika.nonblocking;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     nio non blocking server basic demo
 *     using <code>serverChannel.configureBlocking(false)</code>
 * </p>
 * <p>
 *     once serverSocketChannel configure blocking is false,
 *     channel's accept method is also non blocking
 *     it will return null if no client connect
 * </p>
 * <p>
 *     Note that the client channel(SocketChannel) can also be configured non blocking mode.
 *     it will make the client channel's read method return 0
 * </p>
 * <p>
 *     if no client connect or client didn't send any message, server also need to run
 * </p>
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class BasicServer {
    public static final String HOST = "0.0.0.0";
    public static final int PORT = 8088;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        List<SocketChannel> clientList = new ArrayList<SocketChannel>();

        try(ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(HOST, PORT));
            // change server to non blocking mode
            server.configureBlocking(false);

            while (true) {
                SocketChannel client = server.accept();
                StringBuilder builder = new StringBuilder();
                // server is non blocking, so client maybe null
                if (client != null) {
                    log.info("client connected, {}", client);
                    // this method will make the read method return 0, if client doesn't send any message
                    client.configureBlocking(false);
                    clientList.add(client);
                }
                // scanning all client channel, once find data in any one channel, print the data
                for (SocketChannel clientChannel : clientList) {
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                    int readLength = clientChannel.read(byteBuffer);
                    if (readLength > 0) {
                        byteBuffer.flip();
                        for (int i = 0; i < readLength; i++) {
                            builder.append((char) byteBuffer.get());
                        }
                        builder.append("\n");
                        log.info("form client message is {}", builder);
                        byteBuffer.clear();
                    }
                }
            }
        } catch (IOException ioException) {
            log.error("error be thrown main", ioException);
        }
    }
}
