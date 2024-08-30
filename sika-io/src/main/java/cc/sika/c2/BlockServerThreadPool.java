package cc.sika.c2;

import lombok.extern.slf4j.Slf4j;

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

        // accept client connect

        // process request with independence thread in pool
    }
}
