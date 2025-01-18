package common;

import objectoperation.file.FileModify;
import objectoperation.list.CommonOperation;
import objectoperation.structure.StructureTransform;
import utity.FixedStuff;
import utity.InitialAndTransBlock;
import utity.ProcessingBlock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

public class FinalOperation {
    String indexDir = "";
    static int mutCount = 0;
    static File blockDir;
    public void genAllFiles(FixedStuff fs, int blockCount, String indexDir){
        this.indexDir = indexDir;
        File file = fs.getInitialFile();
        if(blockCount == 0)
            deleteRandomDir(file, indexDir);

        blockDir = new File(indexDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")).replace("_while", "") + "/block_" + blockCount);
        if(blockDir.exists() && blockDir.isDirectory()){
            FileModify fm = new FileModify();
            fm.deleteFolder(blockDir);
        }
        blockDir.mkdirs();
        genMuDir(fs);
        if(Objects.requireNonNull(blockDir.listFiles()).length == 0){
           FileModify fm = new FileModify();
           fm.deleteFolder(blockDir);
           System.out.println("The mutations of this block loop are all invalid.....");
        }
    }

    public void deleteRandomDir(File file, String indexDir){
        File randomFile = new File(indexDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        if(randomFile.exists() && randomFile.isDirectory()){
            FileModify fm = new FileModify();
            fm.deleteFolder(randomFile);
        }
    }


    public void genMuDir(FixedStuff fs){
        int dirCount = 0;
//        List<InitialAndTransBlock> finalItaList = new ArrayList<>();
        System.out.println(fs.getIatList().size());
        for(InitialAndTransBlock iat: fs.getIatList()){
            File dir = new File(blockDir.getAbsolutePath() + "/mutation_" + (dirCount++));
            if(dir.exists() && dir.isDirectory()){
                FileModify fm = new FileModify();
                fm.deleteFolder(dir);
            }
            dir.mkdir();

            genMutation(dir, fs.getInitialFile(), fs.getStartLine(), fs.getEndLine(), iat.getInitialBlock(), "initial");
            mutCount = 0;
            for(ProcessingBlock singleTrans: iat.getTransformedBlockList())
                genMutation(dir, fs.getInitialFile(), fs.getStartLine(), fs.getEndLine(),singleTrans, "transformed");

            //equivalence check
//            System.out.println(indexDir.substring(indexDir.lastIndexOf("/") + 1));
//            EquivalenceCheck ec = new EquivalenceCheck();
//            if(ec.muDirNotPassed(dir)){
//                System.out.println("this muDir abandon....");
//                dirCount--;
//                FileModify fm = new FileModify();
//                fm.deleteFolder(dir);
//            }else{
//                finalItaList.add(iat);
//            }
        }
//        fs.setIatList(finalItaList);
    }

    public void genMutation(File dir, File file, int startLine, int endLine, ProcessingBlock pb, String type){
        System.out.println("genMutation function");
        File newFile;
        if(type.equals("initial")) {
            newFile = new File(dir.getAbsolutePath() + "/" +
                    file.getName().substring(0, file.getName().lastIndexOf(".c")).replace("_while", "") + "_" + type + ".c");
        }else{
            newFile = new File(dir.getAbsolutePath() + "/" +
                    file.getName().substring(0, file.getName().lastIndexOf(".c")).replace("_while", "") + "_" + type + "_" + (mutCount++) + ".c");
        }

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

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
