package cc.sika.blocking;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * <p>NIO Blocking Server</p>
 * <p>create new thread process message for every client</p>
 * This server will accept data once client send one character
 * @author spirit
 * @since 2024-8
 */
@Slf4j
public class BlockServerMultiThread {

    private static final ThreadLocal<ByteBuffer> BUFFER_THREAD_LOCAL =
            ThreadLocal.withInitial(()->ByteBuffer.allocate(1024));
    private static final ThreadLocal<StringBuilder> STRING_BUILDER_THREAD_LOCAL =
            ThreadLocal.withInitial(StringBuilder::new);

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws IOException {
        // create server and bind port 8088
        try (ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.bind(new InetSocketAddress(8088));
            while (true) {
                // accept client connect
                log.debug("waiting for client");
                SocketChannel client = serverSocketChannel.accept();
                log.debug("client connected {}", client);
                // process client request with new thread
                new Thread(()->handleClient(client)).start();
            }
        }
    }

    private static void handleClient(SocketChannel clientChannel) {
        ByteBuffer buffer = BUFFER_THREAD_LOCAL.get();
        StringBuilder stringBuilder = STRING_BUILDER_THREAD_LOCAL.get();

        // read channel data until channel closed.
        try {
            while (clientChannel.isOpen()) {
                int readLength = 0;

                readLength = clientChannel.read(buffer);
                // error channel status
                if (readLength == -1) {
                    log.error("client was disconnected: {}", clientChannel);
                    return;
                }

                // get 1 or n message append to the string
                byte[] apply = new SplitMessage().apply(buffer);
                ByteBuffer wrappedBuffer = ByteBuffer.wrap(apply);
                stringBuilder.append(StandardCharsets.UTF_8.decode(wrappedBuffer));
                log.info("this thread is {}, from client = {}",
                        Thread.currentThread().getName(),
                        stringBuilder);

                // send ack message
                String confirmationMessage = "Message received successfully\n";
                ByteBuffer confirmationBuffer = StandardCharsets.UTF_8.encode(confirmationMessage);

                try {
                    clientChannel.write(confirmationBuffer);
                } catch (IOException e) {
                    log.error("error sending confirmation message to client", e);
                    tryCloseChannel(clientChannel);
                }

                // clear old message
                // buffer for receiving client message will be compacted in SplitMessage's apply method
                stringBuilder.setLength(0);
            }
        } catch (IOException e) {
            log.error("read error, maybe channel was disconnected", e);
        } finally {
            tryCloseChannel(clientChannel);
        }

    }

    private static void tryCloseChannel(SocketChannel clientChannel) {
        try {
            BUFFER_THREAD_LOCAL.remove();
            STRING_BUILDER_THREAD_LOCAL.remove();
            clientChannel.close();
        }
        // fail close channel cause by occurs
        catch (IOException e) {
            log.error("error closing channel", e);
        }
    }

    /**
     * make sure mode of buffer is readable
     * if mode of buffer is write, this method will flip this buffer
     * @param buffer need to be checked
     */
    private static void assertReadable(ByteBuffer buffer) {
        // has some byte, readable
        if(!(buffer.limit() == buffer.capacity() && buffer.position() == 0)) {
            buffer.flip();
        }
    }

    private static boolean isBlank(CharSequence string) {
        return string == null || string.isEmpty() || string.toString().isBlank();
    }

    /**
     * <p>handle half and sticky packets</p>
     * <p>transfer data of buffer to byte array, this behavior will consume all of the complete data of buffer</p>
     * <p>trailing data of buffer that incomplete data will be compact to the front of buffer</p>
     * todo if message just include '\n' or complete message not end of '\n'
     */
    static class SplitMessage implements Function<ByteBuffer, byte[]> {
        @Override
        public byte[] apply(ByteBuffer buffer) {
            assertReadable(buffer);
            ByteBuffer resultBuffer = ByteBuffer.allocate(buffer.remaining());
            // split message if separator be found, put the complete message in new buffer
            for (int i = 0; i < buffer.limit(); i++) {
                if (buffer.get(i) == '\n') {
                    log.debug("found separator, separator position is `{}`", i);
                    int size = i + 1 - buffer.position();
                    for (int j = 0; j < size; j++) {
                        resultBuffer.put(buffer.get());
                    }
                }
            }
            buffer.compact();
            log.debug("after found all separator, invoke compact");
            return resultBuffer.array();
        }

    }
}
