package cc.sika.netty.future;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * show how to use java.util.concurrent.Future with thread pool
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class JDKFuture {
    public static void main(String[] args) {
        // create thread pool
        ExecutorService pool = Executors.newFixedThreadPool(2);

        log.info("submit task");
        Future<Integer> future = pool.submit(new Callable<Integer>() {
            public Integer call() throws Exception {
                Thread.sleep(2000);
                return 1;
            }
        });
        log.info("waiting result");
        // future's get method is blocking, it will block until task complete
        // so here need to wait for task complete (2000ms)
        try {
            log.info("result is {}", future.get());
        } catch (InterruptedException|ExecutionException e) {
            throw new RuntimeException(e);
        }

        pool.shutdown();
    }
}
