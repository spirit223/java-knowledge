package cc.sika.netty.buf;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

/**
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class DifferenceByteBufCreator {
    public static void main(String[] args) {
        ByteBuf heapBuffer = ByteBufAllocator.DEFAULT.heapBuffer();
        ByteBuf directBuffer = ByteBufAllocator.DEFAULT.directBuffer();

        directBuffer.release();

        log.info("heap Buffer = {}", heapBuffer);
        log.info("direct Buffer = {}", directBuffer);
    }
}
