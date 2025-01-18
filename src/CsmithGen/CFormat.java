package CsmithGen;

import ObjectOperation.file.FileModify;
import processtimer.ProcessTerminal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CFormat {
    public void dealComment(File file){
        File delCommentFile = new File(file.getParent() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_delComment.c");
        String command = "cd " + file.getParent() +  " && gcc -fpreprocessed -dD -E " + file.getName() + " > " + delCommentFile.getAbsolutePath();
        ProcessTerminal.voidNotMemCheck(command, "sh");
        file.delete();
        delCommentFile.renameTo(file);
        deleteSpecial(file);
    }

    public void addBrace(File file){
        String command = "cd " + file.getParent() + " && "
                + "clang-tidy " + file.getName() + " --fix -checks=readability-braces-around-statements";
        ProcessTerminal.voidNotMemCheck(command, "sh");
    }

    public void format(File file){
        String command = "cd " + file.getParent() + " && "
                + "clang-format -i " + file.getName();
        ProcessTerminal.voidNotMemCheck(command, "sh");
        FileModify fm = new FileModify();
        List<String> initialList = fm.readFile(file);
        List<String> endList = new ArrayList<String>();

        for(int i = 0; i < initialList.size(); i++){
            String line = initialList.get(i);
            if(line.trim().matches("\\bif\\s*\\((.*)\\)\\s*") && initialList.get(i + 1).trim().startsWith("{")) {
                endList.add(line + " {");
                i++;
                continue;
            }
            endList.add(line);
        }
        fm.writeFile(file, endList);
    }

    public void deleteSpecial(File file) {
        FileModify fm = new FileModify();
        List<String> initialList = fm.readFile(file);
        List<String> endList = new ArrayList<String>();

        for(String line: initialList){
            if(line.trim().matches("#.*\"" + file.getName() + "\"")) {
                continue;
            }
            endList.add(line);
        }
        fm.writeFile(file, endList);
    }
}
