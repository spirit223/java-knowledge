package cc.sika.netty.zeroCopy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;

/**
 * @author spirit
 * @since 2024-09
 */
public class CompositeByteBufFromMultiBuffer {
    public static void main(String[] args) {
        ByteBuf byteBuf1 = ByteBufAllocator.DEFAULT.directBuffer(5);
        ByteBuf byteBuf2 = ByteBufAllocator.DEFAULT.directBuffer(5);

        byteBuf1.writeBytes(new byte[]{1, 2, 3, 4, 5});
        byteBuf2.writeBytes(new byte[]{6, 7, 8, 9, 10});

        // if we use this method, data will be copied twice.
        ByteBuf bufferByAllocate = ByteBufAllocator.DEFAULT.buffer();
        bufferByAllocate.writeBytes(byteBuf1).writeBytes(byteBuf2);

        // this method will not copy data, just combine multiple buffer
        CompositeByteBuf compositeByteBuf = ByteBufAllocator.DEFAULT.compositeBuffer();
        // first parameters decision whether write pointer will be changed
        compositeByteBuf.addComponents(true, byteBuf1, byteBuf2);
    }
}
