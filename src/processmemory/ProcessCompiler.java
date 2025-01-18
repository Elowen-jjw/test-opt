package processmemory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProcessCompiler {
    public static List<String> processNotKillCompiler(String command, int second, String bashType, String aoutName){

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

//    public static List<String> processGcc(String command, int second, String bashType){
//
//        String[] cmd = new String[] { "/bin/" + bashType, "-c", command };
//        ProcessBuilder builder = new ProcessBuilder(cmd);
//        builder.redirectErrorStream(true);
//        Process proc = null;
//        try {
//            proc = builder.start();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        ProcessWorker pw = new ProcessWorker(proc);
//        CheckMemory cm = new CheckMemory(proc);
//        Thread cmThread = new Thread(cm);
//        pw.start();
//        cmThread.start();
//
//        ProcessStatus ps = pw.getPs();
//        try {
//            pw.join(second * 1000);
//            if(ps.exitCode == ps.CODE_STARTED) {
//                pw.interrupt();
//                List<String> result = new ArrayList<String>();
//                proc.destroy();
//                DealMomery.killProcess(proc);
//                cmThread.interrupt();
//                DealMomery.killGccThread();
//                return result;
//            }
//            else {
//                proc.destroy();
//                DealMomery.killProcess(proc);
//                cmThread.interrupt();
//                DealMomery.killGccThread();
//                return ps.output;
//            }
//        }catch(InterruptedException e) {
//            pw.interrupt();
//            proc.destroy();
//            DealMomery.killProcess(proc);
//            cmThread.interrupt();
//            DealMomery.killGccThread();
//            try {
//                throw e;
//            } catch (InterruptedException ex) {
//                throw new RuntimeException(ex);
//            }
//        }
//    }
//
//    public static List<String> processClang(String command, int second, String bashType){
//
//        String[] cmd = new String[] { "/bin/" + bashType, "-c", command };
//        ProcessBuilder builder = new ProcessBuilder(cmd);
//        builder.redirectErrorStream(true);
//        Process proc = null;
//        try {
//            proc = builder.start();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//
//        ProcessWorker pw = new ProcessWorker(proc);
//        CheckMemory cm = new CheckMemory(proc);
//        Thread cmThread = new Thread(cm);
//        pw.start();
//        cmThread.start();
//
//        ProcessStatus ps = pw.getPs();
//        try {
//            pw.join(second * 1000);
//            if(ps.exitCode == ps.CODE_STARTED) {
//                pw.interrupt();
//                List<String> result = new ArrayList<String>();
//                proc.destroy();
//                DealMomery.killProcess(proc);
//                cmThread.interrupt();
//                DealMomery.killClangThread();
//                return result;
//            }
//            else {
//                proc.destroy();
//                DealMomery.killProcess(proc);
//                cmThread.interrupt();
//                DealMomery.killClangThread();
//                return ps.output;
//            }
//        }catch(InterruptedException e) {
//            pw.interrupt();
//            proc.destroy();
//            DealMomery.killProcess(proc);
//            cmThread.interrupt();
//            DealMomery.killClangThread();
//            try {
//                throw e;
//            } catch (InterruptedException ex) {
//                throw new RuntimeException(ex);
//            }
//        }
//    }

}
