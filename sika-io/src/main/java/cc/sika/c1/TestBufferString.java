package cc.sika.c1;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 字符串转为 ByteBuffer
 */
public class TestBufferString {
    public static void main(String[] args) {
        // 将字符串转为buffer
        System.out.println("---------buffer1----------");
        ByteBuffer buffer1 = ByteBuffer.allocate(10);
        buffer1.put("hello".getBytes());
        buffer1.flip();
        System.out.println("buffer1 = " + StandardCharsets.UTF_8.decode(buffer1));

        // 将中文字符串转为buffer
        System.out.println("---------buffer2----------");
        ByteBuffer buffer2 = ByteBuffer.allocate(20);
        buffer2.put("中文".getBytes(StandardCharsets.UTF_8));
        buffer2.flip();
        // 使用中文字符串的getBytes()方法获取字节数组填充进buffer, 使用Charsets进行decode后会乱码, 切换为读模式也会
        System.out.println("buffer2 = " + StandardCharsets.UTF_8.decode(buffer2));

        /* encode 和 wrap 形成的 buffer 的 position 在0的位置, 默认为读模式 */
        // 使用Charsets的encode方法将字符串直接编码并形成一个buffer
        System.out.println("---------buffer3----------");
        ByteBuffer buffer3 = StandardCharsets.UTF_8.encode("hello");

        // 使用Charsets的encode方法将中文字符串直接编码并形成一个buffer
        System.out.println("---------buffer4----------");
        ByteBuffer buffer4 = StandardCharsets.UTF_8.encode("中文");
        System.out.println(StandardCharsets.UTF_8.decode(buffer4));

        ByteBuffer buffer5 = ByteBuffer.wrap("hello".getBytes());

        ByteBuffer buffer6 = ByteBuffer.wrap("中文".getBytes());
        System.out.println("buffer6 = " + StandardCharsets.UTF_8.decode(buffer6));

        System.out.println("中文");
    }
}
