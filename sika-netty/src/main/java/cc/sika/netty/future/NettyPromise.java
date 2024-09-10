package cc.sika.netty.future;

import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;


/**
 * show how to use io.netty.util.concurrent.Promise.
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class NettyPromise {
    public static void main(String[] args) {
        // create pool
        NioEventLoopGroup loopGroup = new NioEventLoopGroup();
        // get executor (thread)
        EventLoop eventLoop = loopGroup.next();
        // promise need one executor to run task
        DefaultPromise<Integer> promise = new DefaultPromise<>(eventLoop);

        // use another thread to simulate task, after completing it need to put result to promise
        loopGroup.next().submit(()->{
            log.info("starting task");
            try {
                Thread.sleep(1000);
                int i = 1/0;
                promise.setSuccess(23);
            } catch (Exception e) {
                // put error message
                promise.setFailure(e);
            }
        });

        log.info("waiting result...");
        // the get method is from JDK's Future interface, so it is blocking
//        try {
//            log.info("result is {}", promise.get());
//        } catch (InterruptedException | ExecutionException e) {
//            throw new RuntimeException(e);
//        }

        // Promise interface extend netty's Future, so it has addListener method
        // use listener can avoid synchronized
        promise.addListener(new GenericFutureListener<Future<? super Integer>>() {
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                if (future.isSuccess()) {
                    log.info("future.get() is {}", future.get());
                    log.info("promise.get() is {}", promise.get());
                }
                else {
                    log.error("promise.get() failed");
                    log.error("failure cause", future.cause());
                }

            }
        });

        loopGroup.shutdownGracefully();
    }
}
