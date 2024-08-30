package cc.sika;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ArrayTest {
    @Test
    public void testArraysToString() {
        byte[] bytes = "中文和English".getBytes(StandardCharsets.UTF_8);
        System.out.println(Arrays.toString(bytes));
        ByteBuffer wrapped = ByteBuffer.wrap(bytes);
        System.out.println(StandardCharsets.UTF_8.decode(wrapped));
    }
}
