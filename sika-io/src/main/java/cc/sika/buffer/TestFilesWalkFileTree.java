package cc.sika.buffer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 遍历文件树操作
 * 对每个文件夹 和 文件进行输出
 * 最后统计文件夹和文件个数
 */
public class TestFilesWalkFileTree {
    private static final Path path = Path.of("D:/develop/Java/jdk-21.0.3");

    public static void main(String[] args) throws IOException {
//        countFile();
        countDll();
    }

    private static void countDll() throws IOException {
        AtomicInteger dllCount = new AtomicInteger(0);
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // 使用Path的endWith判断不出
                if (file.toString().endsWith("dll")) {
                    System.out.println(file.toAbsolutePath());
                    dllCount.incrementAndGet();
                }
                return super.visitFile(file, attrs);
            }
        });
        System.out.println("dllCount = " + dllCount);
    }

    private static void countFile() throws IOException {
        AtomicInteger dirCount = new AtomicInteger(0);
        AtomicInteger fileCount = new AtomicInteger(0);

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                System.out.println("This is dir, dir = " + dir);
                dirCount.incrementAndGet();
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("file = " + file);
                fileCount.incrementAndGet();
                return super.visitFile(file, attrs);
            }
        });

        //dirCount = 87
        //fileCount = 417
        System.out.println("dirCount = " + dirCount);
        System.out.println("fileCount = " + fileCount);
    }
}
