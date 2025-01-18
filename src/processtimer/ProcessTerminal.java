package processtimer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProcessTerminal {

	public static List<String> listNotMemCheck(String command, String bashType){
		Process proc = startProcess(command, bashType);

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
	
	public static void voidNotMemCheck(String command, String bashType){
		Process proc = startProcess(command, bashType);

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

	//isAddTimeout: 当进程运行超时时，是否在返回list中添加timeout string
	//isNeedDelAout: 是否需要在该进程结束时检查并kill含有相关compiler以及aout的进程
	//aoutName: 如果isNeedDelAout为true，则该参数为a.out/csmith/想要kill的进程名字，否则随意
	public static List<String> listMemCheck(String command, int second, String bashType, boolean isAddTimeout,
											boolean isNeedDelSpecificProcs, List<String> delProcessNameList){
		Process proc = startProcess(command, bashType);
		ProcessWorker pw = new ProcessWorker(proc);
		CheckMemory cm = new CheckMemory(proc);
		Thread cmThread = new Thread(cm);
		pw.start();
		cmThread.start();

		ProcessStatus ps = pw.getPs();
		try {
			pw.join(second * 1000L);
			if(ps.exitCode == ps.CODE_STARTED) { //deal with timeout situation
				pw.interrupt();
				List<String> result = new ArrayList<String>();
				if(isAddTimeout) {
					result.add("timeout");
				}
				proc.destroy();
				DealMomery.killProcess(proc);
				if(isNeedDelSpecificProcs) {
					DealMomery.killSpecificPros(delProcessNameList);
				}
				cmThread.interrupt();
				return result;
			}
			else {
				proc.destroy();
				DealMomery.killProcess(proc);
				if(isNeedDelSpecificProcs) {
					DealMomery.killSpecificPros(delProcessNameList);
				}
				cmThread.interrupt();
				return ps.output;
			}
		}catch(InterruptedException e) {
			pw.interrupt();
			proc.destroy();
			DealMomery.killProcess(proc);
			if(isNeedDelSpecificProcs) {
				DealMomery.killSpecificPros(delProcessNameList);
			}
			cmThread.interrupt();
			try {
				throw e;
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		}
    }

	public static Process startProcess(String command, String bashType){
		String[] cmd = new String[] { "/bin/" + bashType, "-c", command };
		ProcessBuilder builder = new ProcessBuilder(cmd);
		builder.redirectErrorStream(true);
		Process proc = null;
		try {
			proc = builder.start();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return proc;
	}

	
}
