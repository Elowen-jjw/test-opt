package common;

import objectoperation.list.CommonOperation;
import overall.OverallProcess;
import sanitizer.SanitizerCheck;
import objectoperation.structure.StructureTransform;
import utity.ProcessingBlock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class MuProcessException {
    public static File genChecksanitizeUBFile(File file, int startLine, int endLine, ProcessingBlock pb){
        File newFile = new File(file.getParent() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_ubcheck.c");
        try {
            if(newFile.exists()){
                newFile.delete();
            }
            newFile.createNewFile();

            FileWriter fw = new FileWriter(newFile, true);
            PrintWriter pw = new PrintWriter(fw);

            List<String> initialFileList = CommonOperation.genInitialList(file);
            int count = 0;
            for(String line: initialFileList){
                count++;
                if(count == 1){
                    if(pb.isAvailableInAddIncludeLib()){
                        pb.getAddIncludeLib().forEach(pw::println);
                    }
                    pw.println(line);
                }
                else if(line.trim().equals("static long __undefined;") && pb.isAvailableInGlobalDeclare()){
                    pw.println(line);
                    pb.getGlobalDeclare().forEach(pw::println);
                }
                else if(count == startLine){
                    if(pb.isAvailableInAddLineBoforeHeader())
                        pb.getAddLineBoforeHeader().forEach(pw::println);
                    if(pb.isAvailableInBlockList())
                        pb.getBlockList().forEach(pw::println);
                }
                else if(count > startLine && count <= endLine){
                }
                else if(line.trim().equals("int main(void) {")){
                    pw.println(line);
                    if(!initialFileList.get(count).contains("int i")){
                        pw.println("int i;");
                    }
                }
                else if(line.trim().matches("func_[0-9]+\\(\\s*\\)\\s*;")
                        && pb.isAvailableInIntoChecksum()){
                    pw.println(line);
                    pb.getIntoChecksum().forEach(pw::println);
                }
                else{
                    pw.println(line);
                }
            }

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

            StructureTransform.formatFile(newFile);
            return newFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static boolean isHaveUB(File file, int startLine, int endLine, ProcessingBlock pb, List<String> ubList){
        File newFile = genChecksanitizeUBFile(file, startLine, endLine, pb);
        try {
            SanitizerCheck sc = new SanitizerCheck(OverallProcess.commandType);
            return sc.filterUB(newFile, ubList);
        }
        finally{
            newFile.delete();
        }
    }
}
