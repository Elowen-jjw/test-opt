package performance;

import java.io.File;

public class Main {
    public static void main(String args[]){
        DirIterator di = new DirIterator();
        File outestDir = new File("/home/elowen/extensionA/CompilerMutationNotCheckMemory");
        di.iteratorMutations(outestDir);
    }
}
