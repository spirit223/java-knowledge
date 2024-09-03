package cc.sika.nonblocking;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static cc.sika.buffer.ByteBufferUtil.debugAll;

/**
 * <p>use multiple thread process connect</p>
 * <p>
 *     make main thread as boss thread to process connection.
 *     if selector triggered by OP_ACCEPT,
 *     boss thread wakeup worker thead, register read/write.
 *     make sure worker create before registering.
 *     just need one boss thread, but worker thread need multiple.
 *     the number of worker threads depends on CPU cores,
 *     it general, the number of threads is the same as the number of CPU cores or refer to Amdahl's law.
 *     Amdahl's law is <pre>S=1/((1-a)+a/n)</pre>
 *     we can get the number of CPU cores by <code>Runtime.getRuntime().availableProcessors()</code>
 * </p>
 * <p>
 *     note: in docker, availableProcessors() method will return numbers of CPU cores of the physical machine, not the number of docker contain
 *     <pre>
 *     fixed it after JDK10
 *     </pre>
 * </p>
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class MultiThreadNonBlockingServer {

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) {
        Thread.currentThread().setName("boss");
        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            Selector bossSelector = Selector.open();
            server.configureBlocking(false);
            server.register(bossSelector, SelectionKey.OP_ACCEPT);
            server.bind(new InetSocketAddress(8088));

            // make sure worker created before register
            Worker[] workers = new Worker[Runtime.getRuntime().availableProcessors()];
            for (int i = 1; i <= workers.length; i++) {
                workers[i-1] = new Worker("worker-" + i);
            }
            // polling load
            AtomicInteger index = new AtomicInteger(0);

            while (true) {
                bossSelector.select();
                Iterator<SelectionKey> iterator = bossSelector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey event = iterator.next();
                    iterator.remove();
                    if (event.isAcceptable()) {
                        SocketChannel clientChannel = server.accept();
                        // register worker
                        clientChannel.configureBlocking(false);
                        log.info("register... [{}]", clientChannel.getRemoteAddress());
                        workers[index.getAndIncrement() % workers.length].register(clientChannel);
                    }
                }
            }

        } catch (IOException e) {
            // todo
            throw new RuntimeException(e);
        }

    }

    static class Worker implements Runnable {
        private final String name;
        private Selector selector;
        // avoid create a lot of thread, one worker has one thread
        private volatile boolean started = false;

        public Worker(String name) {
            this.name = name;
        }

        public void register(SocketChannel socketChannel) throws IOException {
            if (!started) {
                selector = Selector.open();
                Thread thread = new Thread(this, name);
                thread.start();
                started = true;
            }
            selector.wakeup();
            socketChannel.register(selector, SelectionKey.OP_READ);
        }

        @Override
        @SuppressWarnings("InfiniteLoopStatement")
        public void run() {
            while (true) {
                try {
                    selector.select();
                    readIfReadable(selector);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    static class QueueWorker implements Runnable{
        private final String name;
        private Selector selector;
        private volatile boolean started = false;
        private final ConcurrentLinkedQueue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();

        public QueueWorker(String name) {
            this.name = name;
        }

        // it will be invoked by boss thread
        public void register(SocketChannel socketChannel) throws IOException {
            if (!started) {
                selector = Selector.open();
                new Thread(this, name).start();
                started = true;
            }
            // move it to worker thread
            taskQueue.add(()-> {
                try {
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } catch (ClosedChannelException e) {
                    throw new RuntimeException(e);
                }
            });

        }

        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            while (true) {
                try {
                    selector.select();

                    Runnable task = taskQueue.poll();
                    if (!Objects.isNull(task)) {
                        task.run();
                    }

                    readIfReadable(selector);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }


    }

    public static void readIfReadable(Selector selector) throws IOException {
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        if (iterator.hasNext()) {
            SelectionKey event = iterator.next();
            iterator.remove();
            if (event.isReadable()) {
                SocketChannel channel = (SocketChannel) event.channel();
                ByteBuffer allocate = ByteBuffer.allocate(16);
                int readLength = channel.read(allocate);
                if (readLength == -1) {
                    log.warn("client is unlined");
                    event.cancel();
                    channel.close();
                } else {
                    allocate.flip();
                    log.info("read...[{}]", channel.getRemoteAddress());
                    debugAll(allocate);
                }
            }
        }
    }
}
