package processtimer;

import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ProcessWorker extends Thread{
	private Process process;
	private ProcessStatus ps;

	public ProcessWorker(Process process) {
		this.process = process;
		this.ps = new ProcessStatus();
	}
	
	public void run() {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"))) {
			String line = "";
			List<String> outputs = new ArrayList<String>();
			ps.exitCode = ps.CODE_STARTED;
			while((line = br.readLine()) != null) {
//				System.out.println(line);
				outputs.add(line);
			}
			ps.output = outputs;
			ps.exitCode = process.waitFor();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }

	
	public Process getProcess() {
		return process;
	}
	public void setProcess(Process process) {
		this.process = process;
	}
	public ProcessStatus getPs() {
		return ps;
	}
	public void setPs(ProcessStatus ps) {
		this.ps = ps;
	}
	
}
