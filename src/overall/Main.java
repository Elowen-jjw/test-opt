package overall;


import respectfulTestResult.RandomIterator;

import java.io.File;

public class Main {
    // the directory that includes seed programs generating using Csmith
    final static String swarmDir = "...";

    // the directory that includes all mutates and their execution results
    final static String muIndexPath = "...";

    OverallProcess
    public static void main(String args[]){
        OverallProcess overall = new OverallProcess(swarmDir, muIndexPath, "sr");
       overall.process();
   }

    //JustRunLlvm - idea
    // public static void main(String args[]){
    //     JustRunLlvm overall = new JustRunLlvm("/home/sdu/Desktop/idea/CompilerMutationOverall/StrengthReduction_justRunLLVM");
    //     overall.process();
    // }
    
}
