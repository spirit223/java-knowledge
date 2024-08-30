package cc.sika.c1;

import java.nio.ByteBuffer;

/**
 * ByteBuffer 进行读写操作的示例
 * 使用 buffer 的 put 方法将数据内容写入缓冲区
 */
public class ByteBufferReadWriteTest {
    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(10);

        buffer.put((byte) 0x61);

        ByteBufferUtil.debugAll(buffer);

        buffer.put(new byte[]{0x62, 0x63, 0x64});
        ByteBufferUtil.debugAll(buffer);

        buffer.flip();
        System.out.println(buffer.get());
        ByteBufferUtil.debugAll(buffer);

        /*
        * 第一个元素61被读取, 此时pos指针在1的位置上
        * compact 后, 已经被读取的61将被覆盖, 62 63 64三个元素往前移动
        * 位置别为0 1 2
        * 3位置上的元素还是64, 但是pos指针也在3
        * 下一次写入将会覆盖掉3位置上的64
        * */
        buffer.compact();
        ByteBufferUtil.debugAll(buffer);
        System.out.println(buffer.get());
        buffer.put(new byte[]{0x65, 0x66});
        ByteBufferUtil.debugAll(buffer);
    }
}
