package cc.sika.buffer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 分散读取文件中的内容
 *
 * 将读取的内容分散到做个缓冲区中, 可以同时操作多个缓冲区内容提升效率
 */
public class TestScattingRead {
    public static void main(String[] args) {
        try (FileChannel fileChannel = new RandomAccessFile(Objects.requireNonNull(TestScattingRead.class
                .getResource("/worlds.txt")).getFile(),
                "r").getChannel()) {
            ByteBuffer buffer1 = ByteBuffer.allocate(3);
            ByteBuffer buffer2 = ByteBuffer.allocate(3);
            ByteBuffer buffer3 = ByteBuffer.allocate(5);
            fileChannel.read(new ByteBuffer[]{buffer1, buffer2, buffer3});
            buffer1.flip();
            buffer2.flip();
            buffer3.flip();
            System.out.println("buffer1: " + StandardCharsets.UTF_8.decode(buffer1));
            System.out.println("buffer2: " + StandardCharsets.UTF_8.decode(buffer2));
            System.out.println("buffer3: " + StandardCharsets.UTF_8.decode(buffer3));
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }
}
