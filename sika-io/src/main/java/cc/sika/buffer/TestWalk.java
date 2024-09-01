package cc.sika.buffer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Files类的 walk 方法使用演示
 * 使用该方法拷贝一个包含多级文件的文件夹
 * walk 方法返回一个stream流, 提供与 Stream流 交互的能力
 * @see java.nio.file.Files#walk(java.nio.file.Path, java.nio.file.FileVisitOption...)
 */
public class TestWalk {
    private static final String source = "D:" + File.separator + "copy-test" + File.separator + "snipaste";
    public static void main(String[] args) {
        Path sourcePath = Path.of(TestWalk.source);

        try (Stream<Path> walkStream = Files.walk(sourcePath)) {
            walkStream.forEach(path -> {
                // 替换文件路径
                String targetName = path.toString().replace(source, "D:/copy-test/snipaste-copy");
                Path targetPath = Path.of(targetName);
                try {
                    // 目录
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(targetPath);
                        System.out.println("created directory: " + targetPath);
                    }
                    // 文件
                    else if(Files.isRegularFile(path)) {
                        Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("copied file: " + targetPath);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
