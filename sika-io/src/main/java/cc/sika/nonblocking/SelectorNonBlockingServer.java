package cc.sika.nonblocking;

import lombok.extern.slf4j.Slf4j;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import static cc.sika.buffer.ByteBufferUtil.debugAll;

/**
 * <p>nio non blocking server implement by selector</p>
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class SelectorNonBlockingServer {

    public static final String HOST = "0.0.0.0";
    public static final int PORT = 8088;

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        // create server and selector, selector can manager multiple channel
        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            Selector selector = Selector.open();

            // change server to non blocking mode
            server.configureBlocking(false);

            // register server channel to selector(make selector manager server channel)
            SelectionKey registerKey = server.register(selector, 0, null);
            // keep selector just interesting in ACCEPT event
            registerKey.interestOps(SelectionKey.OP_ACCEPT);
            log.debug("register channel to selector, key is [{}]", registerKey);

            server.bind(new InetSocketAddress(HOST, PORT));
            while (true) {
                // blocking server by selector, until any selection key event be triggered
                selector.select();
                // get all key(event)
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                // use iterator avoid concurrent modify exception
                while (iterator.hasNext()) {
                    SelectionKey event = iterator.next();
                    // have to remove key form selectedKeys, consume key will not remove it,
                    // if event is ACCEPT, second invoke accept method will return null
                    iterator.remove();
                    log.debug("in loop ---- get selection key [{}]", event);
                    // get channel that trigger event by SelectionKey's channel() method
                    if (event.isAcceptable()) {
                        // do not use try-with-resources syntax!
                        ServerSocketChannel triggerChannel = (ServerSocketChannel) event.channel();
                        // accept method will consume ACCEPT event,
                        // if not consume, this SelectionKey will cause unlimited loop
                        SocketChannel client = triggerChannel.accept();
                        log.debug("client channel accepted, client is [{}]", client);
                        client.configureBlocking(false);
                        // register new channel to selector,
                        // this is client(socket channel) so interesting in READ / WRITE event
                        // add byte buffer as channel attachment
                        ByteBuffer buffer = ByteBuffer.allocate(16);
                        client.register(selector, SelectionKey.OP_READ, buffer);
                        // interest write and send connection success message
                        ByteBuffer successMessageBuffer = StandardCharsets.UTF_8.encode("connect to server is success");
                        int writeLength = client.write(successMessageBuffer);
                        // if server haven't finished writing all the data at once,
                        // interest in OP_WRITE to prepare for write again
                        // use while will break non-blocking
                        if (successMessageBuffer.hasRemaining()) {
                            event.interestOps(event.interestOps() | SelectionKey.OP_WRITE);
                            // transfer buffer to next write operation
                            event.attach(successMessageBuffer);
                        }

                    }
                    else if (event.isReadable()) {
                        SocketChannel client = (SocketChannel) event.channel();
                        // get buffer from key
                        ByteBuffer buffer = (ByteBuffer) event.attachment();
                        try {
                            int readLength = client.read(buffer);
                            // when client close or shutdown, it will trigger READ event without data
                            if (readLength == -1) {
                                throw new EOFException();
                            }
                            else {
                                split(buffer);
                                // after compact, position equals to limit,
                                // certificate message larger than buffer, dilatation buffer
                                if (buffer.position() == buffer.limit()) {
                                    ByteBuffer newBuffer = ByteBuffer.allocate(buffer.capacity() << 1);
                                    // transfer data of buffer to new buffer
                                    buffer.flip();
                                    newBuffer.put(buffer);
                                    event.attach(newBuffer);

                                }
                            }
                        } catch (IOException e) {
                            log.info("client unlined! ");
                            event.cancel();
                            client.close();
                        }
                    }
                    else if (event.isWritable()) {
                        ByteBuffer attachment = (ByteBuffer) event.attachment();
                        SocketChannel client = (SocketChannel) event.channel();
                        client.write(attachment);
                        // finished writing data, cancel interest in OP_WRITE
                        if (!attachment.hasRemaining()) {
                            event.attach(null);
//                            event.interestOps(event.interestOps() & ~SelectionKey.OP_WRITE);
                            event.interestOps(event.interestOps() ^ SelectionKey.OP_WRITE);
                        }
                    }
                    else {
                        // if you don't do anything, make sure you invoke key's cancel method to consume event
                        // avoid unlimited loop
                        event.cancel();
                    }
                }
            }
        } catch (IOException ioException) {
            log.error("connection error: ", ioException);
        }
    }

    /**
     * split complete message by new line('\n')
     * if split slower than message send, buffer.compact() maybe cause lose data
     * because buffer too small
     * @param buffer data buffer from server to receive client data
     */
    private static void split(ByteBuffer buffer) {
        assertReadable(buffer);
        for (int i = 0; i < buffer.limit(); i++) {
            // 一条消息结束, 拆分后放入一个新的 ByteBuffer
            if (buffer.get(i) == '\n') {
                int size = i + 1 - buffer.position();
                ByteBuffer target = ByteBuffer.allocate(size);
                for (int j = 0; j < size; j++) {
                    target.put(buffer.get());
                }
                target.flip();
//                log.info("split complete message is ==> [{}]", StandardCharsets.UTF_8.decode(target));
                debugAll(target);
            }
        }
        buffer.compact();
    }

    private static void assertReadable(ByteBuffer buffer) {
        if (!(buffer.limit() == buffer.capacity() && buffer.position() == 0)) {
            buffer.flip();
        }
    }
}
