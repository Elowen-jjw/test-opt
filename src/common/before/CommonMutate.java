package common.before;

import ObjectOperation.file.FileModify;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonMutate {
    public void deleteVolatile(File file){
        FileModify fm = new FileModify();
        List<String> initialList = fm.readFile(file);
        List<String> endList = new ArrayList<>();

        boolean isStartMain = false;
        for(String line: initialList){
            if(!line.trim().isEmpty() && Arrays.stream(line.trim().split("\\s+")).anyMatch(word -> word.matches("\\**volatile"))){
                endList.add(line.replaceAll("volatile", ""));
                continue;
            }
            endList.add(line);
        }
        fm.writeFile(file, endList);
    }
}
