package cc.sika.buffer;

import java.nio.ByteBuffer;

/**
 * 控制 ByteBuffer 的 position 指针位置
 * 以及mark字段和reset方法的搭配使用
 */
public class ControllerPosition {
    public static void main(String[] args) {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        buffer.put(new byte[]{'a', 'b', 'c', 'd'});
        buffer.flip();
        System.out.println((char)buffer.get());
        System.out.println((char)buffer.get());
        buffer.mark();
        System.out.println((char)buffer.get());
        System.out.println((char)buffer.get());
        buffer.reset();
        System.out.println((char)buffer.get());
        System.out.println((char)buffer.get());
    }
}
