package cc.sika.c2;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * <p>NIO blocking server</p>
 * server doesn't process message until it receives '\n' character
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class MultiThreadBlockServerPro {
    private static final String HOST = "0.0.0.0";
    private static final int PORT = 8088;
    private static final ThreadLocal<ByteBuffer> BYTE_BUFFER_THREAD_LOCAL =
            ThreadLocal.withInitial(()-> ByteBuffer.allocate(1024));
    private static final ThreadLocal<StringBuilder> STRING_BUILDER_THREAD_LOCAL =
            ThreadLocal.withInitial(StringBuilder::new);


    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        // create server
        try (ServerSocketChannel server = ServerSocketChannel.open()) {
            server.bind(new InetSocketAddress(HOST, PORT));
            // accept connect
            while (true) {
                log.info("waiting for client");
                SocketChannel client = server.accept();
                log.info("client connected {}", client.getRemoteAddress());
                // process client with new thread
                new Thread(()->handleClient(client)).start();
            }
        } catch (IOException ioException) {
            log.error("error be thrown main", ioException);
        }
    }

    /**
     * accept message from client, once receive one byte,
     * the byte will be cased to byte and append to StringBuilder
     * server channel doesn't send ack message until receive '\n'
     * @param client client channel that from server channel accept
     */
    private static void handleClient(SocketChannel client) {
        ByteBuffer buffer = BYTE_BUFFER_THREAD_LOCAL.get();
        StringBuilder stringBuilder = STRING_BUILDER_THREAD_LOCAL.get();

        // read channel data until channel closed
        try {
            while (client.isOpen()) {
                int readLength = client.read(buffer);
                // error client status
                if (readLength == -1) {
                    log.error("client was disconnected {}", client);
                    return ;
                }
                // switch buffer to read mode
                buffer.flip();

                while (buffer.hasRemaining()) {
                    char character = (char) buffer.get();
                    // append and check whether is '\n'
                    stringBuilder.append(character);
                    if (character == '\n') {
                        log.info("thread is {}, from client message is {}",
                                Thread.currentThread().getName(),
                                stringBuilder);
                        try {
                            // send ack message
                            client.write(StandardCharsets.UTF_8.encode("Message received successfully\n"));
                        } catch (IOException exception) {
                            log.error("error sending ack message to client", exception);
                            tryCloseChannel(client);
                        }
                    }
                }

                // clear string builder and buffer for next message
                stringBuilder.setLength(0);
                buffer.clear();
            }
        } catch (IOException e) {
            log.error("read error, maybe channel was disconnected", e);
        } finally {
            tryCloseChannel(client);
        }
    }

    private static void tryCloseChannel(SocketChannel clientChannel) {
        try {
            BYTE_BUFFER_THREAD_LOCAL.remove();
            STRING_BUILDER_THREAD_LOCAL.remove();
            clientChannel.close();
        }
        // fail close channel cause by occurs
        catch (IOException e) {
            log.error("error closing channel", e);
        }
    }
}
