package cc.sika.netty.buf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * alloc ByteBuf
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class ByteBufCreator {
    public static void main(String[] args) {
        ByteBuf buff = ByteBufAllocator.DEFAULT.buffer();

        log.info("buff = {}", buff);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 300; i++) {
            builder.append("a");
        }

        buff.writeBytes(builder.toString().getBytes(StandardCharsets.UTF_8));
        log.info("buff = {}", buff);
    }
}
