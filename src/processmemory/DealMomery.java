package processmemory;

import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.io.IOException;

public class DealMomery {
    public static void killClangThread(){
        try {
            Sigar sigar = new Sigar();

            for (long pid : sigar.getProcList()) {
                ProcState ps = sigar.getProcState(pid);
                if(ps.getName().startsWith("clang") || ps.getName().equals("a.out")){
                    System.out.println(pid + " will be killed............. in the killClangThread");
                    Runtime.getRuntime().exec("kill -9 " + pid);
                }
            }

        } catch (SigarException e) {
            killClangThread();
        } catch (IOException e) {

        }
    }

    public static void killAout(String aoutName){
        try {
            Sigar sigar = new Sigar();

            for (long pid : sigar.getProcList()) {
                ProcState ps = sigar.getProcState(pid);
                if(ps.getName().equals(aoutName)){
                    System.out.println(ps.getName() + " will be killed............. in the aout");
                    Runtime.getRuntime().exec("kill -9 " + pid);
                }
            }

        } catch (SigarException e) {
            killAout(aoutName);
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
