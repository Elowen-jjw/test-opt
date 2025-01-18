package mutationgen;

import testcompareresults.RandomIterator;

import java.io.File;
import java.util.Objects;

public class Main {
    static final String muIndexPath = "/home/elowen/桌面/CompilerMutation";
    public static void main(String args[]){
        //gen mutations in all types that need avarList
//        File sourceDirAll = new File("/home/elowen/桌面/test");
//        GenAllNeedAvar all = new GenAllNeedAvar(sourceDirAll, muIndexPath);
//        all.genMutation();
//
//        //gen Inversion mutations
//        File sourceDirIn = new File("/home/elowen/桌面/testInversion");
//        GenInversion gi = new GenInversion(sourceDirIn, muIndexPath);
//        gi.genMutation();
//
//        //gen Unrolling mutations
//        File sourceDirUn = new File("/home/elowen/桌面/testUnrolling");
//        GenUnrolling gu = new GenUnrolling(sourceDirUn, muIndexPath);
//        gu.genMutation();


        File sourceDirUn = new File("/home/elowen/桌面/test");
        GenFusion gu = new GenFusion(sourceDirUn, muIndexPath, "Max");
        gu.genMutation();

        File dir = new File("/home/elowen/桌面/CompilerMutation/Fusion/Max");
        for(File randomDir: Objects.requireNonNull(dir.listFiles())) {
            if(randomDir.isDirectory() && randomDir.getName().contains("random")) {
                RandomIterator itGcc = new RandomIterator(dir, "gcc");
                itGcc.runSingleRandom(randomDir);
            }
        }

        File sourceDirUn1 = new File("/home/elowen/桌面/test");
        GenFusion gu1 = new GenFusion(sourceDirUn1, muIndexPath, "Add");
        gu1.genMutation();

        File dir1 = new File("/home/elowen/桌面/CompilerMutation/Fusion/Add");
        for(File randomDir: Objects.requireNonNull(dir1.listFiles())) {
            if(randomDir.isDirectory() && randomDir.getName().contains("random")) {
                RandomIterator itGcc = new RandomIterator(dir1, "llvm");
                itGcc.runSingleRandom(randomDir);
            }
        }

        File sourceDirUn2 = new File("/home/elowen/桌面/test");
        GenFusion gu2 = new GenFusion(sourceDirUn2, muIndexPath, "SameHeader");
        gu2.genMutation();

        File dir2 = new File("/home/elowen/桌面/CompilerMutation/Fusion/SameHeader");
        for(File randomDir: Objects.requireNonNull(dir2.listFiles())) {
            if(randomDir.isDirectory() && randomDir.getName().contains("random")) {
                RandomIterator itGcc = new RandomIterator(dir2, "llvm");
                itGcc.runSingleRandom(randomDir);
            }
        }

    }

}
