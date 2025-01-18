package csmithgen;

import objectoperation.file.FileModify;
import objectoperation.list.CommonOperation;
import processmemory.ProcessTerminal;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Format {
    public void dealComment(File file){
        String command = "cd " + file.getParent() + " && "
                + "gcc -fpreprocessed -dD -E " + file.getName() + " > "
                + file.getParent() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_1.c";
        process(command);
        deleteSpecial(file);
    }

    public void addBrace(File file){
        String command = "cd " + file.getParent() + " && "
                + "clang-tidy " + file.getName() + " --fix -checks=readability-braces-around-statements";
        process(command);
    }

    public void format(File file){
        String command = "cd " + file.getParent() + " && "
                + "clang-format -i " + file.getName();
        process(command);
    }

    public void process(String command){
        ProcessTerminal pt = new ProcessTerminal();
        pt.processThreadNotLimitJustExec(command, "sh");
    }

    public void deleteSpecial(File file) {
        FileModify fm = new FileModify();
        List<String> initialList = fm.readFile(file);
        List<String> endList = new ArrayList<String>();

        for(String line: initialList){
            System.out.println(line);
            if(line.trim().matches("#.*\"" + file.getName() + "\"")) {
                System.out.println(line);
                continue;
            }
            endList.add(line);
        }
        CommonOperation.printList(endList);
        fm.writeFile(file, endList);
    }
}
