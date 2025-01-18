package processmemory;

import org.hyperic.sigar.Mem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessTerminal {

	public List<String> processThreadTimeLimit(String command, int second, String bashType){
		
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
		pw.start();
		ProcessStatus ps = pw.getPs();
		try {
			pw.join(second * 1000);
			if(ps.exitCode == ps.CODE_STARTED) {
				pw.interrupt();
				List<String> result = new ArrayList<String>();
				proc.destroy();
				if(proc.isAlive()){
					System.out.println("kill process " + proc.pid());
					Runtime.getRuntime().exec("kill -9 " + proc.pid());
				}
				return result;
			}
			else {
				proc.destroy();
				if(proc.isAlive()){
					System.out.println("kill process " + proc.pid());
					Runtime.getRuntime().exec("kill -9 " + proc.pid());
				}
				return ps.output;
			}
		}catch(InterruptedException e) {
			pw.interrupt();
			proc.destroy();
			if(proc.isAlive()){
				System.out.println("kill process " + proc.pid());
				try {
					Runtime.getRuntime().exec("kill -9 " + proc.pid());
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
			try {
				throw e;
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
	
	public List<String> processThreadNotLimit(String command, String bashType){
		
		String[] cmd = new String[] { "/bin/"+bashType, "-c", command };
		ProcessBuilder builder = new ProcessBuilder(cmd);
		builder.redirectErrorStream(true);
		Process proc = null;
		try {
			proc = builder.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		ProcessWorker pw = new ProcessWorker(proc);
		pw.start();
		ProcessStatus ps = pw.getPs();
		try {
			pw.join();
			if(ps.exitCode == ps.CODE_STARTED) {
				pw.interrupt();
				List<String> result = new ArrayList<String>();
				proc.destroy();
				return result;
			}
			else {
				proc.destroy();
				return ps.output;
			}
		}catch(InterruptedException e) {
			pw.interrupt();
			proc.destroy();
			try {
				throw e;
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
	
	public void processThreadNotLimitJustExec(String command, String bashType){
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
		pw.start();
		ProcessStatus ps = pw.getPs();
		try {
			pw.join();
			if(ps.exitCode == ps.CODE_STARTED) {
				pw.interrupt();
			}
		}catch(InterruptedException e) {
			pw.interrupt();
			try {
				throw e;
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}finally {
			proc.destroy();
		}
	}

	public List<String> execTerminal(String command, int second, String bashType){
		try {
			List<String> resultLines = new ArrayList<String>();
			String[] cmd = new String[] { "/bin/" + bashType, "-c", command };
			String execLine = "";
			ProcessBuilder builder = new ProcessBuilder(cmd);
			builder.redirectErrorStream(true);
			Process proc = builder.start();

			if (!proc.waitFor(second, TimeUnit.SECONDS)) {
				proc.destroy();
				return new ArrayList<String>();
			}

			InputStream ins = proc.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
			while((execLine = br.readLine()) != null) {
				resultLines.add(execLine);
			}

			proc.destroy();

			return resultLines;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}


	public void killThread(){
		try {
			Sigar sigar = new Sigar();

			for (long pid : sigar.getProcList()) {
				ProcState ps = sigar.getProcState(pid);
				if(ps.getName().startsWith("")){
					System.out.println(pid + " will be killed.............");
					Runtime.getRuntime().exec("kill -9 " + pid);
				}
			}

		} catch (SigarException e) {
			killThread();
		} catch (IOException e) {

		}
	}


	public void printMemInfo(){
		try {
			Sigar sigar = new Sigar();
			Mem mem = sigar.getMem();

			System.out.println("内存实际占用情况： " + mem.getActualUsed() / 1024 / 1024 /1024  + "g");
			System.out.println("内存实际空闲情况： " + mem.getActualFree() / 1024 / 1024 /1024  + "g");
		}  catch (SigarException e) {
			throw new RuntimeException(e);
		}
	}
	
}
