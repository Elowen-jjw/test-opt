package ObjectOperation.file;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FileSort {
    public static void renameFilesBySize(String directoryPath) {
        File directory = new File(directoryPath);

        if (!directory.isDirectory()) {
            System.out.println("The provided path is not a directory.");
            return;
        }

        File[] files = directory.listFiles();
        System.out.println(files.length);
        if (files == null || files.length == 0) {
            System.out.println("No .c files found in the directory.");
            return;
        }

        // Sort files by size (smallest to largest)
        Arrays.sort(files, Comparator.comparingLong(File::length));

        // Rename files
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            File newFile = new File(directory, "random" + i + ".c");
            if (file.renameTo(newFile)) {
                System.out.println("Renamed: " + file.getName() + " -> " + newFile.getName());
            } else {
                System.out.println("Failed to rename: " + file.getName());
            }
        }
    }

    public static void main(String[] args) {
//        String directoryPath = "/home/sdu/Desktop/test"; // Replace with your directory path
//        renameFilesBySize(directoryPath);
        printInfo();
    }

    public static void printInfo(){
        String directoryPath = "/home/sdu/Desktop/cvise-test/random_notfunc";
        File foler = new File(directoryPath);
        FileInfo fi = new FileInfo();
        List<File> allFileList = fi.getSortedOverallFiles(foler);
        List<Integer> numbers = new ArrayList<>();
        for(File file: allFileList){
            if(file.getName().matches("random\\d+_copy\\.c")){
                numbers.add((int) (file.length()/1024));
                System.out.println(file.getName() + ": " + file.length()/1024);
            }
        }
        System.out.println("middle value is " + findMedian(numbers));
    }

    public static double findMedian(List<Integer> numbers) {
        // 先对列表进行排序
        Collections.sort(numbers);

        int size = numbers.size();
        // 如果大小是偶数，中位数是中间两个元素的平均值
        if (size % 2 == 0) {
            return (numbers.get(size / 2 - 1) + numbers.get(size / 2)) / 2.0;
        } else {
            // 如果大小是奇数，中位数是中间的那个元素
            return numbers.get(size / 2);
        }
    }
}
