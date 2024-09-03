package cc.sika.buffer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * 粘包和半包解决思路
 * 场景:
 * 网络传输的原始数据为3条
 * Hello, world\n
 * I'm YiHuan\n
 * Haw are you?\n
 * 13 + 11 + 12
 * 由于传输缓冲区或者网络问题变成两个内容:
 * Hello, world\nI'm YiHuan\nHo
 * w are you?\n
 * 解决思路:
 * 每次接收到数据后将数据进行拆分, 最后剩余的无法拆分的内容压缩到缓冲区头部与后续数据进行拼接
 */
public class TestStickyHalfPack {
    public static void main(String[] args) {
        ByteBuffer source = ByteBuffer.allocate(36);
        source.put("Hello, world\nI'm YiHuan\nHo".getBytes(StandardCharsets.UTF_8));
        split(source);
        source.put("w are you?\n".getBytes(StandardCharsets.UTF_8));
        split(source);
    }

    private static void split(ByteBuffer source) {
        split(source,(byteBuffer -> System.out.print(StandardCharsets.UTF_8.decode(byteBuffer))));
    }

    private static void split(ByteBuffer source, Consumer<ByteBuffer> operator) {
        source.flip();

        for (int i = 0; i < source.limit(); i++) {
            // 一条消息结束, 拆分后放入一个新的 ByteBuffer
            if (source.get(i) == '\n') {
                int size = i + 1 - source.position();
                ByteBuffer target = ByteBuffer.allocate(size);
                for (int j = 0; j < size; j++) {
                    target.put(source.get());
                }
                target.flip();
                String targetContent = StandardCharsets.UTF_8.decode(target).toString();
                System.out.println(targetContent);
            }
        }

        source.compact();
    }
}
