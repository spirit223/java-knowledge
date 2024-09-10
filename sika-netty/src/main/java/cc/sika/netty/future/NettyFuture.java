package cc.sika.netty.future;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;

/**
 * show how to use io.netty.util.concurrent.Future with EventLoopGroup(as thread pool)
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class NettyFuture {
    public static void main(String[] args) {
        // create event loop group(like thread pool)
        EventLoopGroup group = new NioEventLoopGroup();
        EventLoop eventLoop = group.next();

        log.info("submit task");
        Future<Integer> nettyFuture = eventLoop.submit(() -> {
            Thread.sleep(1000);
            return 1;
        });
        log.info("waiting for result");

        // netty's future is blocking
        // it has to wait for task completing and return result
        /*try {
            log.info("result is {}", nettyFuture.get());
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }*/

        // use listener and future's getNow method to obtain the task result asynchronously
        nettyFuture.addListener(new GenericFutureListener<Future<? super Integer>>() {
            @Override
            public void operationComplete(Future<? super Integer> future) throws Exception {
                log.info("result is {}", future.getNow());
            }
        });

        // shutdown all thread in group
        group.shutdownGracefully();
    }
}
