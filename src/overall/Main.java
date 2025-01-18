package overall;


public class Main {
    final static String swarmDir = "/home/sdu/Desktop/swarm";
    final static String muIndexPath = "/home/sdu/Desktop/CompilerMutationOverall";

    public static void main(String args[]) {
        //fusion_sameheader fusion_add fusion_max   unswitching_compound
        OverallProcess overall = new OverallProcess(swarmDir, muIndexPath, "fusion_max");
        overall.process();
    }

}
