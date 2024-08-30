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
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * NIO 阻塞服务器代码
 * 为每个客户端连接创建一个线程处理客户端数据
 * @author spirit
 * @since 2024-8
 */
@Slf4j
public class BlockServerMultiThread {

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
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        StringBuilder stringBuilder = new StringBuilder();

        // read channel data until channel closed.
        while (clientChannel.isOpen()) {
            int readLength = 0;

            try {
                readLength = clientChannel.read(buffer);
                // error channel status
                if (readLength == -1) {
                    log.error("client was disconnected: {}", clientChannel);
                    tryCloseChannel(clientChannel);
                    return;
                }
            } catch (IOException e) {
                log.error("read error, maybe channel was disconnected", e);
                tryCloseChannel(clientChannel);
            }

            // get 1 or n message append to the string
            byte[] apply = new SplitMessage().apply(buffer);
            ByteBuffer wrappedBuffer = ByteBuffer.wrap(apply);
            stringBuilder.append(StandardCharsets.UTF_8.decode(wrappedBuffer));
            log.info("this thread is {}, " +
                            "form client = {}",
                    Thread.currentThread().getName(),
                    stringBuilder);
            // clear old message
            stringBuilder.setLength(0);
            // data processing completed, close channel?
            // tryCloseChannel(clientChannel);
        }
    }

    private static void tryCloseChannel(SocketChannel clientChannel) {
        try {
            clientChannel.close();
        }
        // fail close channel cause by occurs
        catch (IOException e) {
            log.error("error closing channel", e);
        }
    }


    /**
     * handle half and sticky packets
     * split client message with '\n'
     * once newline be found, data will be input the operator and invoke
     *
     * @param buffer data buffer, type of byteBuffer
     * @param operator Function(ByteBuffer, ByteBuffer), accept buffer, once message will be function invoke
     * @param string input and save string
     */
    private static void splitMessage(ByteBuffer buffer, Consumer<ByteBuffer> operator, CharSequence string) {
        // 1. change buffer mode, if not mode of readable
        assertReadable(buffer);

        // 2. iterate buffer by get(i), if character '\n' be found, split message and put in temp buffer
        for (int i = 0; i < buffer.limit(); i++) {
            if(buffer.get(i) == '\n') {
                int size = i + 1 - buffer.position();
                ByteBuffer saveDataBuffer = ByteBuffer.allocate(size);
                for (int j = 0; j < size; j++) {
                    saveDataBuffer.put(buffer.get(j));
                }
                saveDataBuffer.flip();
                assertReadable(saveDataBuffer);
                // get complete message, mark position guarantees that no data will be lost due to other operations
                saveDataBuffer.mark();
                operator.accept(saveDataBuffer);
                saveDataBuffer.reset();
                /* change to string and append to the char sequence */
                String decode = StandardCharsets.UTF_8.decode(saveDataBuffer).toString();
                // char sequence not a null, append message
                if (!isBlank(string)) {
                    if (string instanceof StringBuilder) {
                        ((StringBuilder) string).append(decode);
                    }
                    if (string instanceof String) {
                        string = string + decode;
                    }
                }
                // create new string
                else {
                    string = decode;
                }
            }
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
     */
    static class SplitMessage implements Function<ByteBuffer, byte[]> {
        @Override
        public byte[] apply(ByteBuffer buffer) {
            assertReadable(buffer);
            List<Byte> resultList = new ArrayList<>();
            // split message if separator be found, put the complete message in new buffer
            for (int i = 0; i < buffer.limit(); i++) {
                if (buffer.get(i) == '\n') {
                    log.debug("found separator, separator position is `{}`", i);
                    int size = i + 1 - buffer.position();
                    ByteBuffer tempBuffer = ByteBuffer.allocate(size);
                    for (int j = 0; j < size; j++) {
//                        tempBuffer.put(buffer.get(j));
                        tempBuffer.put(buffer.get());
                    }
                    byte[] array = tempBuffer.array();
                    for (byte b : array) {
                        resultList.add(b);
                    }
                }
            }
            buffer.compact();
            log.debug("after found all separator, invoke compact");
            byte[] bytes = new byte[resultList.size()];
            for (int i = 0; i < resultList.size(); i++) {
                bytes[i] = resultList.get(i);
            }
            log.debug("all byte is {}", bytes);
            return bytes;
        }

    }
}
