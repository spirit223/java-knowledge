package cc.sika.buffer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Files工具类的使用演示
 */
public class TestFiles {
    public static void main(String[] args) {
        System.out.println("\n");
//        fileExists();
//        createOneLevelDir();
//        tryCreateMoreLevelDir();
//        createManyLevelDir();
        copyFile();
    }

    private static void fileExists() {
        Path path = Path.of("1/2.txt");
        boolean exists = Files.exists(path);
        // D:\develop\project\java\java-knowledge\1\2.txt exists: false
        System.out.println(path.toFile().getAbsolutePath() + " exists: " + exists);
    }

    private static void createOneLevelDir() {
        final String dirName = "create-by-Files";
        Path path1 = Path.of(dirName);
        try {
            File file = Files.createDirectory(path1).toFile();
            // D:\develop\project\java\java-knowledge\create-by-Filesexists: true
            System.out.println(file.getAbsolutePath() + " exists: " + Files.exists(path1));
        } catch (IOException e) {
            System.err.println("Could not create directories: " + e);
            throw new RuntimeException(e);
        }
    }

    private static void tryCreateMoreLevelDir() {
        final String dirName = "create-by-Files/1/2";
        Path path1 = Path.of(dirName);
        try {
            File file = Files.createDirectory(path1).toFile();
            // D:\develop\project\java\java-knowledge\create-by-Filesexists: true
            System.out.println(file.getAbsolutePath() + " exists: " + Files.exists(path1));
        } catch (IOException e) {
            System.err.println("Could not create directories: " + e);
            throw new RuntimeException(e);
        }
    }

    private static void createManyLevelDir() {
        final String dirName = "create-by-Files/1/2";
        Path path1 = Path.of(dirName);
        try {
            File file = Files.createDirectories(path1).toFile();
            // D:\develop\project\java\java-knowledge\create-by-Files\1\2 exists: true
            System.out.println(file.getAbsolutePath() + " exists: " + Files.exists(path1));
        } catch (IOException e) {
            System.err.println("Could not create directories: " + e);
            throw new RuntimeException(e);
        }
    }

    private static void copyFile() {
        final String sourceName = "source.txt";
        final String targetName = "target.txt";
        Path sourcePath = Path.of(sourceName);
        Path targetPath = Path.of(targetName);
        try {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
//            Files.copy(sourcePath, targetPath);
            // copy is successful! D:\develop\project\java\java-knowledge\target.txt
            System.out.println("copy is successful! " + targetPath.toFile().getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void moveFile() {
        final String sourceName = "source.txt";
        final String targetName = "target.txt";
        Path sourcePath = Path.of(sourceName);
        Path targetPath = Path.of(targetName);
        try {
            Files.move(sourcePath, targetPath, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void deleteFile() {
        final String targetName = "target.txt";
        Path targetPath = Path.of(targetName);
        try {
            Files.delete(targetPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
