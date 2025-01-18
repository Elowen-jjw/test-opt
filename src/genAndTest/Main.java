package genAndTest;

import identicalTestResult.DirIterator;

import java.io.File;

public class Main {
    static final String muIndexPath = "/home/elowen/extensionA/CompilerMutation";
    public static void main(String args[]){
//        checkIndexLastRandom(muIndexPath + "/mutations.unrolling");
//        File sourceDir = new File("/home/nicai/桌面/testUnrolling");
//        GenAndTestUnrolling tu = new GenAndTestUnrolling(sourceDir, muIndexPath);
//        tu.genAndTestMutation();

//        checkIndexLastRandom(muIndexPath + "/mutations.inversion");
//        File sourceDir1 = new File("/home/nicai/桌面/testInversion");
//        GenAndTestInversion ti = new GenAndTestInversion(sourceDir1, muIndexPath);
//        ti.genAndTestMutation();

//        checkIndexLastRandom(muIndexPath + "/Fusion_CheckUB/SameHeader");
//        checkIndexLastRandom(muIndexPath + "/Fusion_CheckUB/Add");
//        checkIndexLastRandom(muIndexPath + "/Fusion_CheckUB/Max");
//        File sourceDir2 = new File("/home/elowen/桌面/testFusion");
//        GenAndTestFusion all = new GenAndTestFusion(sourceDir2, muIndexPath);
//        all.genAndTestMutation();

//        checkIndexLastRandom(muIndexPath + "/Unswitching/OneVar");
//        checkIndexLastRandom(muIndexPath + "/Unswitching/TwoVar");
//        File sourceDir3 = new File("/home/elowen/桌面/test");
//        GenAndTestUnswitching gtUn = new GenAndTestUnswitching(sourceDir3, muIndexPath);
//        gtUn.genAndTestMutation();
    	
        checkIndexLastRandom(muIndexPath + "/Invariant");
        File sourceDir4 = new File("/home/elowen/桌面/test");
        GenAndTestInvariant gtInvariant = new GenAndTestInvariant(sourceDir4, muIndexPath);
        gtInvariant.genAndTestMutation();

        System.out.println("end!");
    }

    public static void checkIndexLastRandom(String outestPath){
        File outermostDir = new File(outestPath);
        DirIterator dt = new DirIterator(outermostDir);
        dt.checkLatestRandomEnd();
    }
}
