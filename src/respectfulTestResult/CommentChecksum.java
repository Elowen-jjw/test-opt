package respectfulTestResult;

import ObjectOperation.file.FileModify;
import ObjectOperation.file.getAllFileList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;


public class CommentChecksum {
	public void AddComment(File sourceDir) {
		List<File> allFileList = new ArrayList<File>();

		getAllFileList getFileList = new getAllFileList(sourceDir);
		getFileList.getAllFile(sourceDir, allFileList);
		getFileList.compareFileList(allFileList);
		int fineCnt = 0;
		for(File file: allFileList) {
		    if(!file.getName().endsWith(".c")) {
		        continue;
		    }
		    System.out.println(fineCnt ++ + ": " + file.getName());
		    FileModify fileModify = new FileModify();
		    List<String> fileList = fileModify.readFile(file);
		    CommentOut(file, fileList);
		}
	}
	
	public void CommentOut(File file, List<String> fileList) {
        try {
            if(file.exists()) file.delete();
            file.createNewFile();
            
            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fos);
            //tempFile.deleteOnExit();

            boolean isMain = false;
            boolean isReturn = false;
            for(String line: fileList) {
            	if(isMain) {
            		if(isReturn || line.matches("\\s*func_[0-9]+\\(.*\\);\\s*")) ;
            		else if(line.contains("return 0;")) {
            			isReturn = true;
            		}
            		else {
            			line = "//" + line;
            		}
            		pw.println(line);
            		continue;
            	}
            	else if(line.contains("int main(void) {")) {
                    isMain = true;
                }
            	pw.println(line);
            }
           

            pw.flush();
            pw.close();

        }catch(Exception e){
            e.printStackTrace();
        }
    }


}
