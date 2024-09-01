package cc.sika.buffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * ByteBuffer 使用示例
 * 通过 FileChannel 从文件中读取内容
 * 将读取的内容存放到缓冲区中
 */
public class ByteBufferTest {
    public static void main(String[] args) {

        URL resource = ByteBufferTest.class.getResource("/data.txt");
        assert resource != null;
        try (
                FileInputStream fileInputStream = new FileInputStream(resource.getFile());
                // 从流中获取通道
                FileChannel channel = fileInputStream.getChannel()
        ) {
            // 申请缓冲区, 新申请的缓冲区位置为0, 可以进行写操作 -- HeapByteBuffer
            ByteBuffer buffer = ByteBuffer.allocate(10);
            // 将管道中的内容写入缓冲区
            int len = channel.read(buffer);
            while (len >= 0) {
                // 因为写操作, 此时指针不在缓冲区的起始位置, 需要翻转一下才可以从头开始读
                buffer.flip();
                // hasRemaining -- 判断是否有元素可以读取
                while (buffer.hasRemaining()) {
                    byte b = buffer.get();
                    System.out.println(((char) b));
                }
                // 清楚缓冲区内容并调整指针到起始位置
                buffer.clear();
                len = channel.read(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
