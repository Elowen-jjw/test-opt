package processmemory;


import overall.OverallProcess;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class LoopExecValues {
	//for() for(;;);
	public static int getTimes(File file, List<String> initialList, int startLine, int endLine) {
		try {
			Set<Integer> numSet = new HashSet<>();
			int loopExecTimes = 0;
			String filename = file.getName();
			String newExecFileName = filename.substring(0, filename.lastIndexOf(".")) + "_execTime" + filename.substring(filename.lastIndexOf("."));
			File newFile = new File(file.getParent() + "/" + newExecFileName);
			if (newFile.exists()) {
				newFile.delete();
			}

			newFile.createNewFile();


			FileWriter fw = new FileWriter(newFile, true);
			PrintWriter pw = new PrintWriter(fw);

			for (int i = 1; i <= initialList.size(); i++) {
				if (i == startLine) {
					pw.println("int countLoopTimes = 0;");
					pw.println(initialList.get(i - 1));
					pw.println("countLoopTimes++;");
					continue;
				}
				if (i == endLine) {
					pw.println(initialList.get(i - 1));
					pw.println("printf(\"countLoopTimes = %d\\n\", countLoopTimes);");
					continue;
				}
				pw.println(initialList.get(i - 1));
			}

			pw.flush();
			fw.flush();
			pw.close();
			fw.close();

			String aoutFilename = newFile.getName().substring(0, newFile.getName().lastIndexOf(".c"));

			String command = (OverallProcess.commandType.equals("gcc")? "export LANGUAGE=en && export LANG=en_US.UTF-8 && ": "")
					+ "cd " + file.getParent() + " && " + OverallProcess.commandType + " " + newExecFileName
					+ " -lm -I $CSMITH_HOME/include -o " + aoutFilename;
			ProcessTerminal pt = new ProcessTerminal();
			pt.processThreadNotLimitJustExec(command, "sh");

			File aoutFile = new File(file.getParent() + "/" + aoutFilename);
			if(!aoutFile.exists()){
				newFile.delete();
				return 0;
			}

			command = "cd " + file.getParent() + " && " + "./" + aoutFilename;

			List<String> execLines = ProcessCompiler.processNotKillCompiler(command, 30, "sh", aoutFilename);

			deleteAoutFile(file, aoutFilename);

			if (execLines.isEmpty()) {
				newFile.delete();
				return 0;
			}

			for (String s : execLines) {
				if (s.contains("countLoopTimes")) {
					String timesStr = "";
					for (int i = 0; i < s.length(); i++) {
						if ((s.charAt(i) >= 48) && (s.charAt(i) <= 57)) {
							timesStr += s.charAt(i);
						}
					}
					try {
						numSet.add(Integer.parseInt(timesStr));
					} catch (NumberFormatException e) {
						newFile.delete();
						return 0;
					}
				}
			}

			if(numSet.size() == 1){
				Iterator<Integer> setIt = numSet.iterator();
				while(setIt.hasNext()){
					loopExecTimes = setIt.next();
				}
			}
			else{
				loopExecTimes = 0;
			}

			newFile.delete();
			return loopExecTimes;

		}catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean checkConsistency(File file, List<String> initialList, int startLine, int endLine) {
		try {
			Set<Integer> numSet1 = new HashSet<>();
			Set<Integer> numSet2 = new HashSet<>();
			int loopExecTimes1 = 0;
			int loopExecTimes2 = 0;
			String filename = file.getName();
			String newExecFileName = filename.substring(0, filename.lastIndexOf(".")) + "_consist" + filename.substring(filename.lastIndexOf("."));
			File newFile = new File(file.getParent() + "/" + newExecFileName);
			if (newFile.exists()) {
				newFile.delete();
			}

			newFile.createNewFile();


			FileWriter fw = new FileWriter(newFile, true);
			PrintWriter pw = new PrintWriter(fw);

			for (int i = 1; i <= initialList.size(); i++) {
				if (i == startLine) {
					pw.println("int countLoopTimes1 = 0;");
					pw.println("int countLoopTimes2 = 0;");
					pw.println(initialList.get(i - 1));
					pw.println("countLoopTimes1++;");
					continue;
				}
				if (i == endLine) {
					pw.println("countLoopTimes2++;");
					pw.println(initialList.get(i - 1));
					pw.println("printf(\"countLoopTimes1 = %d\\n\", countLoopTimes1);");
					pw.println("printf(\"countLoopTimes2 = %d\\n\", countLoopTimes2);");
					continue;
				}
				pw.println(initialList.get(i - 1));
			}

			pw.flush();
			fw.flush();
			pw.close();
			fw.close();

			String aoutFilename = newFile.getName().substring(0, newFile.getName().lastIndexOf(".c"));

			String command = (OverallProcess.commandType.equals("gcc")? "export LANGUAGE=en && export LANG=en_US.UTF-8 && ": "")
					+ "cd " + file.getParent() + " && " + OverallProcess.commandType + " " + newExecFileName
					+ " -lm -I $CSMITH_HOME/include -o " + aoutFilename;

			ProcessTerminal pt = new ProcessTerminal();
			pt.processThreadNotLimitJustExec(command, "sh");

			File aoutFile = new File(file.getParent() + "/" + aoutFilename);
			if(!aoutFile.exists()){
				newFile.delete();
				return false;
			}

			command = "cd " + file.getParent() + " && " + "./" + aoutFilename;

			List<String> execLines = ProcessCompiler.processNotKillCompiler(command, 30, "sh", aoutFilename);

			deleteAoutFile(file, aoutFilename);

			if (execLines.isEmpty()) {
				newFile.delete();
				return false;
			}

			for (String s : execLines) {
				if (s.contains("countLoopTimes1")) {
					String timesStr = "";
					s = s.replace("countLoopTimes1", "");
					for (int i = 0; i < s.length(); i++) {
						if ((s.charAt(i) >= 48) && (s.charAt(i) <= 57)) {
							timesStr += s.charAt(i);
						}
					}
					try {
						numSet1.add(Integer.parseInt(timesStr));
					} catch (NumberFormatException e) {
						newFile.delete();
						return false;
					}
				}
				else if (s.contains("countLoopTimes2")) {
					String timesStr = "";
					s = s.replace("countLoopTimes2", "");
					for (int i = 0; i < s.length(); i++) {
						if ((s.charAt(i) >= 48) && (s.charAt(i) <= 57)) {
							timesStr += s.charAt(i);
						}
					}
					try {
						numSet2.add(Integer.parseInt(timesStr));
					} catch (NumberFormatException e) {
						newFile.delete();
						return false;
					}
				}
			}

			if(numSet1.size() == 1){
				Iterator<Integer> setIt = numSet1.iterator();
				while(setIt.hasNext()){
					loopExecTimes1 = setIt.next();
				}
			}
			else{
				newFile.delete();
				return false;
			}

			if(numSet2.size() == 1){
				Iterator<Integer> setIt = numSet2.iterator();
				while(setIt.hasNext()){
					loopExecTimes2 = setIt.next();
				}
			}
			else{
				newFile.delete();
				return false;
			}
//			System.out.println(loopExecTimes1);
//			System.out.println(loopExecTimes2);

			if(loopExecTimes1 == loopExecTimes2){
				newFile.delete();
				return true;
			}
			else {
				newFile.delete();
				return false;
			}

		}catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void deleteAoutFile(File file, String aoutFilename){
		File outFile = new File(file.getParent() + "/" + aoutFilename);
		if(outFile.exists()){
			outFile.delete();
		}
	}
	
}
