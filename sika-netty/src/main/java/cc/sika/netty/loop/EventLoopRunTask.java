package cc.sika.netty.loop;

import io.netty.channel.EventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * using event loop to process default task and schedule task
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class EventLoopRunTask {
    public static void main(String[] args) {
        // create EventLoop, use EventLoop need by group.
        // NioEventLoopGroup can process io event,schedule task, default task
        // DefaultEventLoopGroup can not process io event
        EventLoopGroup group = new NioEventLoopGroup(2);
        // EventLoopGroup group = new NioEventLoopGroup();
        // EventLoopGroup group2 = new DefaultEventLoopGroup();

        // get event loop by next() and process default task
        log.info("first EventLoop: [{}]", group.next());
        log.info("second EventLoop: [{}]", group.next());
        log.info("third EventLoop: [{}]", group.next());
        log.info("fourth EventLoop: [{}]", group.next());

        // invoke default task by submit() or execute()
        EventLoop eventLoop = group.next();
        eventLoop.submit(() -> log.info("submit by event loop (running default task)"));

        // eventLoop interface extended ScheduledExecutorService, so it can invoke schedule task
        // invoke schedule task with scheduleAtFixedRate() or scheduleAtFixedDelay()
        // delay one seconds log row message
        group.next().scheduleAtFixedRate(()-> log.info("schedule by event loop (running schedule task)"),
                0, 1, TimeUnit.SECONDS);

        // thread of event loop run task not the same as main
        log.info("main thread");
    }
}
