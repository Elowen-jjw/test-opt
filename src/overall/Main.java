package overall;


import respectfulTestResult.RandomIterator;

import java.io.File;

public class Main {
    final static String swarmDir = "/home/sdu/Desktop/swarm";
    final static String muIndexPath = "/home/sdu/Desktop/idea/CompilerMutationOverall";

    //OverallProcess
//    public static void main(String args[]){
//        OverallProcess overall = new OverallProcess(swarmDir, muIndexPath, "sr");
//        overall.process();
//    }

    //JustRunLlvm - idea
    public static void main(String args[]){
        JustRunLlvm overall = new JustRunLlvm("/home/sdu/Desktop/idea/CompilerMutationOverall/StrengthReduction_justRunLLVM");
        overall.process();
    }

    //记得改aoutname in testcompiler
}
