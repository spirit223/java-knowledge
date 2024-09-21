package cc.sika.netty.zeroCopy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @author spirit
 * @since 2024-09
 */
@Slf4j
public class ByteBufSlice {
    public static void main(String[] args) {
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.directBuffer();

        byteBuf.writeBytes(new byte[]{'a','b','c','d','e','f','g','h','i','j'});
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(0, bytes, 0, bytes.length);
        // now, byteBuf is [a, b, c, d, e, f, g, h, i, j]
        log.info("byteBuf's content = {}", byteAyyToChar(bytes));

        ByteBuf slice1 = byteBuf.slice(0, 5);
        slice1.retain();
        byte[] bytes1 = new byte[5];
        slice1.getBytes(0, bytes1, 0, bytes1.length);
        ByteBuf slice2 = byteBuf.slice(5, 5);
        slice2.retain();
        byte[] bytes2 = new byte[5];
        slice2.getBytes(0, bytes2, 0, bytes2.length);
        // slice1 is [a, b, c, d, e]
        log.info("slice1's content = {}", byteAyyToChar(bytes1));
        // slice2 is [f, g, h, i, j]
        log.info("slice2's content = {}", byteAyyToChar(bytes2));

        // change first character to '1' of slice1, see whether character was changed of byteBuf
        slice1.setByte(0, '1');
        log.info("after change slice1");
        bytes = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(0, bytes, 0, byteBuf.readableBytes());
        // byteBuf = [1, b, c, d, e, f, g, h, i, j]
        log.info("byteBuf = {}", byteAyyToChar(bytes));
        bytes1 = new byte[5];
        slice1.getBytes(0, bytes1, 0, bytes1.length);
        // slice1 = [1, b, c, d, e]
        log.info("slice1 = {}", byteAyyToChar(bytes1));
        bytes2 = new byte[5];
        slice2.getBytes(0, bytes2, 0, bytes2.length);
        // slice2 = [f, g, h, i, j]
        log.info("slice2 = {}", byteAyyToChar(bytes2));
        slice1.release();
        slice2.release();
        byteBuf.release();
    }

    public static char[] byteAyyToChar(byte[] chars) {
        ByteBuffer byteBuffer = ByteBuffer.allocate(chars.length);
        byteBuffer.put(chars);
        byteBuffer.flip();
        return StandardCharsets.UTF_8.decode(byteBuffer).array();
    }
}
