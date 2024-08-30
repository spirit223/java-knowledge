package cc.sika.c1;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Objects;

/**
 * 演示 FileChannel 的 transferTo() 方法
 *
 * @see java.nio.channels.FileChannel#transferTo
 */
public class TestFileChannelTransferTo {
    public static final String source = "/transfer-source.txt";
    public static final String target = "./transfer-target.txt";

    public static void main(String[] args) {
        try (FileInputStream fileInputStream =
                     new FileInputStream(Objects.requireNonNull(TestFileChannelTransferTo.class.getResource(source)).getFile());
             FileOutputStream fileOutputStream =
                     new FileOutputStream(target)) {
            try (FileChannel from = fileInputStream.getChannel();
            FileChannel to = fileOutputStream.getChannel()) {
                // 因为 transferTo 使用系统的零拷贝实现, 最大传输能力不超过2g, 需要结合transferTo的实际传输字节数完成完整数据传输
                long count = from.size();
                long left = from.size();
                while (left > 0) {
                    long writeBytes = from.transferTo(count - left, left, to);
                    left -= writeBytes;
                    System.out.println("transferred " + writeBytes + " bytes");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
