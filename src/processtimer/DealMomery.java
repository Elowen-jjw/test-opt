package processtimer;

import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class DealMomery {

    public static void killSpecificPros(List<String> processNameList){
        try {
            Sigar sigar = new Sigar();

            for (long pid : sigar.getProcList()) {
                ProcState ps = sigar.getProcState(pid);
                for(String procName: processNameList) {
                    if (ps.getName().equals(procName)) {
                        System.out.println(ps.getName() + " will be killed............. in the specific process deletion......");
                        Runtime.getRuntime().exec("kill -9 " + pid);
                    }
                }
                //deal with qemu-riscv64
                if(ps.getName().contains("qemu-riscv64")){
                    long startTime = getProcessStartTime(sigar, pid);
                    // 判断启动时间是否超过 40s（40,000 毫秒）
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - startTime > 40000) {
                        System.out.println("Killing process: " + ps.getName() + " (PID: " + pid + ")");
                        System.out.println("Process start time: " + new Date(startTime));
                        Runtime.getRuntime().exec("kill -9 " + pid);
                    } else {
                        System.out.println(ps.getName() + " (PID: " + pid + ")" + "starts not over 40 seconds");
                    }
                }
            }
        } catch (SigarException e) {
            killSpecificPros(processNameList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long getProcessStartTime(Sigar sigar, long pid) {
        try {
            ProcTime procTime = sigar.getProcTime(pid);
            // 返回进程的启动时间（以毫秒为单位）
            return procTime.getStartTime();
        } catch (SigarException e) {
            System.err.println("Error getting process start time for PID: " + pid);
            return -1;
        }
    }

    public static void killClangThread(){
        try {
            Sigar sigar = new Sigar();

            for (long pid : sigar.getProcList()) {
                ProcState ps = sigar.getProcState(pid);
                if(ps.getName().startsWith("clang") || ps.getName().equals("a.out") || ps.getName().contains("qemu-riscv64")){
                    System.out.println(pid + " will be killed............. in the killClangThread");
                    Runtime.getRuntime().exec("kill -9 " + pid);
                }
            }

        } catch (SigarException e) {
            killClangThread();
        } catch (IOException e) {

        }
    }

    public static void killGccThread(){
        try {
            Sigar sigar = new Sigar();

            for (long pid : sigar.getProcList()) {
                ProcState ps = sigar.getProcState(pid);
                if(ps.getName().startsWith("gcc") || ps.getName().equals("a.out")){
                    System.out.println(pid + " will be killed............. in the killGccThread");
                    Runtime.getRuntime().exec("kill -9 " + pid);
                }
            }

        } catch (SigarException e) {
            killGccThread();
        } catch (IOException e) {

        }
    }

    public static void killProcess(Process proc){
        try {
            Runtime.getRuntime().exec("kill -9 " + proc.pid());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
