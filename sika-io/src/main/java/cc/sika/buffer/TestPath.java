package cc.sika.buffer;

import java.io.File;
import java.nio.file.Path;

/**
 * 路径描述接口Path的演示
 * @see java.nio.file.Path
 * @see Path#of(String, String...)
 * @see java.nio.file.Path#normalize
 * @see java.nio.file.Path#toFile
 * @see java.io.File#getAbsolutePath
 */
public class TestPath {
    public static void main(String[] args) {
        Path path1 = Path.of("1.txt");                      // 相对路径, 相对于user.dir环境变量
        Path path2 = Path.of("d:\\1.txt");                  // 绝对路径
        Path path3 = Path.of("d:/1.txt");                   // 绝对路径
        Path path4 = Path.of("d:\\data", "projects");// 绝对路径
        Path path5 = Path.of("/data", "projects");   // 绝对路径, getAbsolutePath时会将最前的/替换为根路径

        // 使用path.toFile()可以通过Path对象可以直接获取位置描述对应的File对象
        File file1 = path1.toFile();
        File file2 = path2.toFile();
        File file3 = path3.toFile();
        File file4 = path4.toFile();
        File file5 = path5.toFile();
        // System.getProperty("user.dir") = D:\develop\project\java\java-knowledge
        System.out.println("System.getProperty(\"user.dir\") = " + System.getProperty("user.dir"));
        // path1 = 1.txt
        System.out.println("path1 = " + path1);
        // path2 = d:\1.txt
        System.out.println("path2 = " + path2);
        // path3 = d:\1.txt
        System.out.println("path3 = " + path3);
        // path4 = d:\data\projects
        System.out.println("path4 = " + path4);
        // path5 = \data\projects
        System.out.println("path5 = " + path5);
        // file1 = 1.txt
        System.out.println("file1 = " + file1);
        // file2 = d:\1.txt
        System.out.println("file2 = " + file2);
        // file3 = d:\1.txt
        System.out.println("file3 = " + file3);
        // file4 = d:\data\projects
        System.out.println("file4 = " + file4);
        // file5 = D:\data\projects
        System.out.println("file5 = " + file5.getAbsolutePath());
    }
}
