package objectoperation.file;

import processmemory.ProcessTerminal;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileModify {

	public List<String> readFile(File file){
		List<String> initList = new ArrayList<String>();
		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
			String thisLine;

			while((thisLine = bf.readLine()) != null) {
				initList.add(thisLine);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return initList;
	}

	public void writeFile(File file, List<String> finalList){
		try {
			FileOutputStream fos = new FileOutputStream(file);
			PrintWriter pw = new PrintWriter(fos);
			for(String s: finalList){
				pw.println(s);
			}
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

	//指定行前/后添加内容
	public void addLinesToFile(File file, Map<Integer, List<String>> addLines, boolean isFront)	{
		List<String> initalList = readFile(file);
		List<String> endList = new ArrayList<String>();

		if(isFront) {
			for(int i = 1; i <= initalList.size(); i++){
				if(addLines.containsKey(i)){
					List<String> addLine = addLines.get(i);
					addLine.forEach(s -> {
						endList.add(s);
					});
				}
				endList.add(initalList.get(i-1));
			}
		}
		else {
			for(int i = 1; i <= initalList.size(); i++){
				endList.add(initalList.get(i-1));
				if(addLines.containsKey(i)){
					List<String> addLine = addLines.get(i);
					addLine.forEach(s -> {
						endList.add(s);
					});
				}
			}
		}
		writeFile(file, endList);
	}

	//删除指定行内容
	public void deleteLinesToFile(File file, Set<Integer> numberList)	{
		List<String> initalList = readFile(file);
		List<String> endList = new ArrayList<String>();

		for(int i = 1; i <= initalList.size(); i++){
			if(numberList.contains(i)){
				continue;
			}
			endList.add(initalList.get(i-1));
		}
		writeFile(file, endList);
	}

	//修改指定行内容
	public void updateLinesToFile(File file, Map<Integer, List<String>> addLines)	{
		List<String> initalList = readFile(file);
		List<String> endList = new ArrayList<String>();

		for(int i = 1; i <= initalList.size(); i++){
			if(addLines.containsKey(i)){
				List<String> addLine = addLines.get(i);
				addLine.forEach(s -> {
					endList.add(s);
				});
				continue;
			}
			endList.add(initalList.get(i-1));
		}

		writeFile(file, endList);
	}

	public void copyFile(File newFile, File oldFile){
		try {
			FileReader fr = new FileReader(oldFile);
			BufferedReader br = new BufferedReader(fr);

			FileWriter fw = new FileWriter(newFile);
			PrintWriter pw = new PrintWriter(fw);

			String temp = "";
			while((temp = br.readLine())!=null){
				pw.println(temp);
				pw.flush();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteFolder(File file){
		String command = "rm -r " + file.getAbsolutePath();
		ProcessTerminal pt = new ProcessTerminal();
		pt.processThreadNotLimitJustExec(command, "sh");
	}
}
