package common;

import astinfo.model.IfStatement;
import astinfo.model.LoopStatement;
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
import java.util.*;

public class AllBlockChange {
    String indexDir;
    int mutCount = 0;
    File blockDir;
    File mutDir;
    File initialFile;
    Map<Integer, Integer> saeMap = new HashMap<>();
    int dirCount = 0;
    int dirAverage = 0;
    public void genAllFiles(List<FixedStuff> fsList, int blockCount, String indexDir){//fsList is the most outer loop
        this.indexDir = indexDir;
        initialFile = fsList.get(0).getInitialFile();
        blockDir = new File(indexDir + "/" +
                initialFile.getName().substring(0, initialFile.getName().lastIndexOf(".c")).replace("_while", "") + "/block_" + blockCount);
        if(blockDir.exists() && blockDir.isDirectory()){
            FileModify fm = new FileModify();
            fm.deleteFolder(blockDir);
        }
        blockDir.mkdirs();

        setSaeMap(fsList);
        setDirAverage(fsList);
//        System.out.println("average:  " + this.dirAverage);

        List<InitialAndTransBlock> itaTemp = new ArrayList<>();
        chooseCombineIta(fsList, itaTemp);

        if(Objects.requireNonNull(blockDir.listFiles()).length == 0){
            FileModify fm = new FileModify();
            fm.deleteFolder(blockDir);
            System.out.println("The mutations of this block loop are all invalid.....");
        }
    }
    
    public void setDirAverage(List<FixedStuff> fsList) {
    	int cnt = 1;
    	int max = 0;
    	for(FixedStuff fs: fsList) {
    		cnt *= fs.getIatList().size();
    		max = Math.max(max, fs.getIatList().size());
    	}
    	if(cnt <= 100) {
    		this.dirAverage = max;
    	}else {
    		this.dirAverage = (int) Math.ceil(Math.pow(100, (double)1/fsList.size()));
            this.dirAverage = Math.max(2, this.dirAverage);
    	}
    }

    public void setSaeMap(List<FixedStuff> fsList){
        for(FixedStuff fs: fsList){
            saeMap.put(fs.getStartLine(), fs.getEndLine());
        }
    }

    public void chooseCombineIta(List<FixedStuff> fsList, List<InitialAndTransBlock> iatTemp) {
        if(dirCount > 99) return;
    	int cnt = iatTemp.size();
    	if(cnt == fsList.size()) {
    		genMuSameResult(iatTemp);
    		return ;
    	}
    	Set<Integer> itaIndexSet = new HashSet<>();
    	Random random = new Random();
    	int itaListSize = fsList.get(cnt).getIatList().size();
    	int index;
    	do {
    		index = random.nextInt(itaListSize);
    		if(itaIndexSet.contains(index)) continue ;
    		else itaIndexSet.add(index);
    		List<InitialAndTransBlock> newItaTemp = new ArrayList<>(iatTemp);
    		newItaTemp.add(fsList.get(cnt).getIatList().get(index));
    		chooseCombineIta(fsList, newItaTemp);
    	}while(itaIndexSet.size() < dirAverage && itaIndexSet.size() < itaListSize);
    	
    }
    
    public void genMuSameResult(List<InitialAndTransBlock> itaTemp) {
        mutDir = new File(blockDir.getAbsolutePath() + "/mutation_" + (dirCount++));
        if(mutDir.exists() && mutDir.isDirectory()){
            FileModify fm = new FileModify();
            fm.deleteFolder(mutDir);
        }
        mutDir.mkdir();

        List<ProcessingBlock> initialPBList = new ArrayList<>();

        List<List<ProcessingBlock>> transPbLists = new ArrayList<>();

        for(InitialAndTransBlock singleIta: itaTemp){
            initialPBList.add(singleIta.getInitialBlock());
            transPbLists.add(singleIta.getTransformedBlockList());
        }

        genMutation(initialPBList, "initial");

        //transformed_mutCount.c
        mutCount = 0;
        List<ProcessingBlock> oneTransList = new ArrayList<>();
        chooseCombineTrans(transPbLists, oneTransList);

        //equivalence check
//        System.out.println(indexDir.substring(indexDir.lastIndexOf("/") + 1));
//        EquivalenceCheck ec = new EquivalenceCheck();
//        if(ec.muDirNotPassed(mutDir)){
//            dirCount--;
//            FileModify fm = new FileModify();
//            fm.deleteFolder(mutDir);
//        }
    }

    public void chooseCombineTrans(List<List<ProcessingBlock>> transPbLists, List<ProcessingBlock> oneTransList) {
        if(mutCount > 99) return;
        int cnt = oneTransList.size();
        if(cnt == transPbLists.size()) {
            genSingleTrans(oneTransList);
            return ;
        }
        for(ProcessingBlock singleTrans: transPbLists.get(cnt)) {
            List<ProcessingBlock> newTransList = new ArrayList<>(oneTransList);
            newTransList.add(singleTrans);
            chooseCombineTrans(transPbLists, newTransList);
        }
    }

    public void genSingleTrans(List<ProcessingBlock> oneTransList){
        genMutation(oneTransList, "transformed");
    }

    public void genMutation(List<ProcessingBlock> pbList, String type){
        File newFile;
        if(type.equals("initial")) {
            newFile = new File(mutDir.getAbsolutePath() + "/" +
                    initialFile.getName().substring(0, initialFile.getName().lastIndexOf(".c")).replace("_while", "") + "_" + type + ".c");
        }else{
            newFile = new File(mutDir.getAbsolutePath() + "/" +
                    initialFile.getName().substring(0, initialFile.getName().lastIndexOf(".c")).replace("_while", "") + "_" + type + "_" + (mutCount++) + ".c");
        }
        try {
            if(newFile.exists()){
                newFile.delete();
            }
            newFile.createNewFile();

            List<String> allIncludeLib = new ArrayList<>();
            List<String> allGlobalDeclare = new ArrayList<>();
            List<String> allIntoChecksum = new ArrayList<>();

            for(ProcessingBlock pb: pbList){
                if(pb.isAvailableInAddIncludeLib()){
                    allIncludeLib.addAll(pb.getAddIncludeLib());
                }
                if(pb.isAvailableInGlobalDeclare()){
                    allGlobalDeclare.addAll(pb.getGlobalDeclare());
                }
                if(pb.isAvailableInIntoChecksum()){
                    allIntoChecksum.addAll(pb.getIntoChecksum());
                }
            }

            FileWriter fw = new FileWriter(newFile, true);
            PrintWriter pw = new PrintWriter(fw);

            List<String> initialFileList = CommonOperation.genInitialList(initialFile);
            int count = 0;
            int startLine = 0;
            int endLine = 0;
            int loopId = -1;
            for(String line: initialFileList){
                count++;
                if(count == 1){
                    if(!allIncludeLib.isEmpty()){
                        allIncludeLib.forEach(pw::println);
                    }
                    pw.println(line);
                }
                else if(line.trim().equals("static long __undefined;") && !allGlobalDeclare.isEmpty()){
                    pw.println(line);
                    allGlobalDeclare.forEach(pw::println);
                }
                else if(saeMap.containsKey(count)){
                    startLine = count;
                    endLine = saeMap.get(startLine);
                    loopId++;
                    if(pbList.get(loopId).isAvailableInAddLineBoforeHeader())
                        pbList.get(loopId).getAddLineBoforeHeader().forEach(pw::println);
                    if(pbList.get(loopId).isAvailableInBlockList())
                        pbList.get(loopId).getBlockList().forEach(pw::println);
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
                        && !allIntoChecksum.isEmpty()){
                    pw.println(line);
                    allIntoChecksum.forEach(pw::println);
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

    public List<FixedStuff> getLoopAvailableFsList(List<FixedStuff> fsList, List<LoopStatement> loopList){
        List<FixedStuff> afsList = new ArrayList<>();
        for(int i=0; i< fsList.size(); i++){
            if(fsList.get(i).getIatList().isEmpty()){
                continue;
            }
            if(loopList.get(i).getParentLoopStmt() == null){
                afsList.add(fsList.get(i));
            }
        }
        return afsList;
    }

    public List<FixedStuff> getIfAvailableFsList(List<FixedStuff> fsList, List<IfStatement> ifList){
        List<FixedStuff> afsList = new ArrayList<>();
        for(int i=0; i< fsList.size(); i++){
            if(ifList.get(i).getParentIfStmt() == null){
                afsList.add(fsList.get(i));
            }
        }
        return afsList;
    }

}
