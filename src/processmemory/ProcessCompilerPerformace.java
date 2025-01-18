package processmemory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProcessCompilerPerformace {
    //run random.c don't output the checksum, i.e. add comment in main function, just test the exception and error and execution times
    public static List<String> process(String command, int second, String bashType, String aoutName){//add timeout if timeout

        String[] cmd = new String[] { "/bin/" + bashType, "-c", command };
        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.redirectErrorStream(true);
        Process proc = null;
        try {
            proc = builder.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ProcessWorker pw = new ProcessWorker(proc);
        CheckMemory cm = new CheckMemory(proc);
        Thread cmThread = new Thread(cm);
        pw.start();
        cmThread.start();

        ProcessStatus ps = pw.getPs();
        try {
            pw.join(second * 1000);
            if(ps.exitCode == ps.CODE_STARTED) {
                pw.interrupt();
                List<String> result = new ArrayList<String>();
                result.add("timeout");
                proc.destroy();
                DealMomery.killProcess(proc);
                DealMomery.killAout(aoutName);
                cmThread.interrupt();
                return result;
            }
            else {
                proc.destroy();
                DealMomery.killProcess(proc);
                DealMomery.killAout(aoutName);
                cmThread.interrupt();
                return ps.output;
            }
        }catch(InterruptedException e) {
            pw.interrupt();
            proc.destroy();
            DealMomery.killProcess(proc);
            DealMomery.killAout(aoutName);
            cmThread.interrupt();
            try {
                throw e;
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
