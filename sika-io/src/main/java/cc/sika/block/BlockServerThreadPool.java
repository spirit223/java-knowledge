package cc.sika.block;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>Blocking Server by NIO</p>
 * <p>process client request with independence thread form thread pool</p>
 * @author spirit
 * @since 2024-08
 */
@Slf4j
public class BlockServerThreadPool {
    public static void main(String[] args) {
        // create thread pool, allocate some thread
        // which is the best choice not to use fixedThreadPoolExecutor?
        try(ThreadPoolExecutor pool = new ThreadPoolExecutor(10, 20,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100), new SikaTreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());) {

        }

        // accept client connect

        // process request with independence thread in pool
    }

    static class SikaTreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable task) {
            return new Thread(task, "Custom Thread #" + poolNumber.getAndIncrement());
        }
    }

}
