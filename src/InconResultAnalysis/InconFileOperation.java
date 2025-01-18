package InconResultAnalysis;

import ObjectOperation.file.FileModify;
import common.CommonInfoFromFile;
import common.PropertiesInfo;
import processtimer.ProcessTerminal;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class InconFileOperation {
	public static void main(String[] args) {
		InconFileOperation ifo = new InconFileOperation();
//		ifo.filterIncon();
		ifo.filterSanitize();
	}

	public void runReduced(){
		InconReduced ir = new InconReduced("clang");
		File inconFolder = new File(PropertiesInfo.hostMap.get("statute").get(1) + "/clangInco/clang_output_incon");
		ir.multiRun(inconFolder);
	}

	public void filterSanitize() {
		ExecutorService executor = Executors.newFixedThreadPool(PropertiesInfo.Santizer_ThreadCount); // 创建一个包含 10 个线程的线程池

		for (File randomDir : findRandomFolders(new File(PropertiesInfo.inconResultIndexDir + "/statute"))) {
			// 提交任务到线程池
			executor.submit(() -> sanitizeDirectory(randomDir));
		}

		executor.shutdown(); // 关闭线程池
		// 等待所有任务完成
//		try {
//			if (!executor.awaitTermination(60, TimeUnit.MINUTES)) {
//				executor.shutdownNow(); // 超时后强制关闭
//			}
//		} catch (InterruptedException e) {
//			executor.shutdownNow(); // 中断后强制关闭
//		}
	}

	private void sanitizeDirectory(File randomDir) {
		for (File file : randomDir.listFiles()) {
			if (!file.getName().endsWith(".c")) continue;
			if (!CommonInfoFromFile.checkCorrection(file)) {
				file.delete();
			}
		}
		if (!isValidRandomDir(randomDir)) {
			FileModify fm = new FileModify();
			fm.deleteFolder(randomDir);
		}
	}

	public void filterIncon(){
		for(String mutateType: PropertiesInfo.hostMap.keySet()){
			File sourcePath = PropertiesInfo.hostMap.get(mutateType).get(0);
			File destPath = PropertiesInfo.hostMap.get(mutateType).get(1);
			if(!sourcePath.exists() || !sourcePath.isDirectory()) continue;
			moveSingleCompilerIncon(sourcePath, destPath, "gcc");
			moveSingleCompilerIncon(sourcePath, destPath, "clang");
			System.out.println("mutate " + mutateType + " end move Incon!!!!!!!!!!!!!!");
		}
	}

	public void moveSingleCompilerIncon(File sourcePath, File destPath, String compilerType){
		if(!sourcePath.exists() || sourcePath.listFiles().length == 0) {
			return;
		}
		if(!destPath.exists()){
			destPath.mkdirs();
		}

		File compilerResultFolder = new File(sourcePath + "/" + compilerType);
		if(!compilerResultFolder.exists()){
			System.out.println("compilerTxt: " + compilerType + " not exist! " + compilerResultFolder.getAbsolutePath());
			return;
		}

		String command = "cp -r " + compilerResultFolder.getAbsolutePath() + " " + destPath;	//move gcc or llvm folder
		ProcessTerminal.voidNotMemCheck(command, "sh");

		List<String> inconTxtList = new ArrayList<String>();
		for(File txtFile: compilerResultFolder.listFiles()) {
			String txtName =  txtFile.getName();
			if(!txtName.endsWith(".txt")) continue;
			inconTxtList.add(txtName.substring(0,txtName.lastIndexOf(".txt")));
		}

		for(String inconTxt: inconTxtList) {
			File sourceTxt = new File(sourcePath + "/" + compilerType + "/" + inconTxt + ".txt");
			if(!sourceTxt.exists()) {
				continue;
			}

			File destTxtFolder = new File(destPath + "/" + compilerType + "Inco/"+  inconTxt);
			if(!destTxtFolder.exists()) {
				destTxtFolder.mkdirs();
			}

			List<String> inconMutationList = readFile(sourceTxt);
			for(String inconMutation: inconMutationList) {
				command = "cp -r " + sourcePath + inconMutation + " " + destTxtFolder;		//move Incon Mutation Folder
				ProcessTerminal.voidNotMemCheck(command, "sh");
			}
		}
	}

	public List<String> readFile(File file){
		List<String> initList = new ArrayList<String>();
		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
			String thisLine;

			while((thisLine = bf.readLine()) != null) {
				initList.add(thisLine.trim());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return initList;
	}

	public List<File> findRandomFolders(File dir) {
		List<File> result = new ArrayList<>();
		if (!dir.isDirectory()) {
			return result;
		}

		File[] files = dir.listFiles();
		if (files != null) {
			Pattern pattern = Pattern.compile("random\\d+");
			for (File file : files) {
				if (file.isDirectory() && pattern.matcher(file.getName()).matches()) {
					result.add(file);
				}
				result.addAll(findRandomFolders(file));
			}
		}
		return result;
	}

	public boolean isValidRandomDir(File randomDir){
		int count = 0;
		for(File file: randomDir.listFiles()){
			if(file.getName().matches("random\\d+_\\d+\\.c"))
				count++;
		}
		return count != 0;
	}

}
