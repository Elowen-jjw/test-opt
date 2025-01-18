package overall;

import ObjectOperation.file.FileModify;
import ObjectOperation.file.getAllFileList;
import csmith.SwarmGen;
import genMutation.*;
import respectfulTestResult.RandomIterator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//把opt step删除掉 重新跑变体的clang指令
public class JustRunLlvm {
    String outermostPath;
    String muType;
    public static String commandType = "clang";

    public JustRunLlvm(String outermostPath){
        this.outermostPath = outermostPath;
    }

    public void process(){
        File outermostFile = new File(outermostPath);
        File[] dirs = Objects.requireNonNull(outermostFile.listFiles());
        Arrays.sort(dirs);
        for(File randomDir: dirs){
            if(randomDir.isDirectory() && randomDir.getName().matches("random[0-9]+")){
                genTestResult(randomDir);
            }
        }
    }

    public void genTestResult(File randomDir){
        File outermostDir = new File(outermostPath);

        //llvm
        if(commandType.equals("clang")) {
            RandomIterator itLlvm = new RandomIterator(outermostDir, "llvm");
            itLlvm.runSingleRandom(randomDir);
        }
    }
}
