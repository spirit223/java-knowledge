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
                        client.register(selector, SelectionKey.OP_READ);
                    } else if (event.isReadable()) {
                        SocketChannel client = (SocketChannel) event.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(16);
                        try {
                            int readLength = client.read(buffer);
                            // when client close or shutdown, it will trigger READ event without data
                            if (readLength == -1) {
                                throw new EOFException();
                            }
                            else {
                                buffer.flip();
                                debugAll(buffer);
                            }
                        } catch (IOException e) {
                            log.info("client unlined! ");
                            event.cancel();
                            client.close();
                        }
                    } else {
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
}
