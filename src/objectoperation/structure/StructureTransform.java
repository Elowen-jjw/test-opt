package objectoperation.structure;

import astinfo.AstStmtOperation;
import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.Inform_Gen.LoopInform_Gen;
import astinfo.model.LoopStatement;
import objectoperation.file.FileModify;
import objectoperation.file.getAllFileList;
import processmemory.ProcessTerminal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class StructureTransform {
    public static void main(String args[]) {
        File dir = new File("/home/nicai/桌面/initialPart");
        List<File> allFileList = new ArrayList<File>();

        getAllFileList getFileList = new getAllFileList(dir);
        getFileList.getAllFile(dir, allFileList);
        getFileList.compareFileList(allFileList);

        for (File file : allFileList) {
            if (!file.getName().endsWith(".c")) {
                continue;
            }
            System.out.println(file.getName());
            forTransWhile(file, "/home/nicai/桌面/while");
//            checkFor(ObjectOperation.file);
        }
       
    }
    
    public static void checkFor(File file) {
    	AstInform_Gen astgen = new AstInform_Gen(file);
    	LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);
        for(LoopStatement loop: loopList) {
        	if(loop.getStmtType().equals("forStmt")) {
        		System.out.println(file.getName());
        		break;
        	}
        }
    }

    public static File forTransWhile(File file, String dir) {
        File whileFile = new File(dir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_while.c");
        if (whileFile.exists()) {
            whileFile.delete();
        }
        try {
            whileFile.createNewFile();
            FileModify fm = new FileModify();
            fm.copyFile(whileFile, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<Integer, Integer> edge = new HashMap<Integer, Integer>();

        AstInform_Gen astgen = new AstInform_Gen(whileFile);
//        if (astgen.astList.size() == 0) return null;

        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);
        for (LoopStatement singleLoop : loopList) {
            edge.put(singleLoop.getStartLine(), singleLoop.getEndLine());
        }

        String regexFor = "\\bfor\\s*\\((.*);(.*);(.*?)\\)\\s*\\{";
        Pattern pFor = Pattern.compile(regexFor);
        Matcher mFor;

        Map<Integer, List<String>> updateLines = new HashMap<Integer, List<String>>();
        Map<Integer, List<String>> addLines = new HashMap<Integer, List<String>>();
        FileModify fm = new FileModify();
        List<String> initialList = fm.readFile(whileFile);
        for (Integer i : edge.keySet()) {
            String forhead = initialList.get(i - 1);
            int endLine = edge.get(i);
            mFor = pFor.matcher(forhead);
            if (mFor.find()) {
                //for( int i = 0; i < n; i++)
                List<String> modify_line1 = new ArrayList<String>();
                List<String> modify_line2 = new ArrayList<String>();
                String exp1 = mFor.group(1);
                String exp2 = mFor.group(2);
                String exp3 = mFor.group(3);

                if (!exp2.matches("\\s*")) {
                    modify_line1.add(exp1.trim() + ";   " + "while (" + exp2.trim() + ") {");
                } else {
                    modify_line1.add(exp1.trim() + ";   " + "while (1) {");
                }
                updateLines.put(i, modify_line1);

                if (!exp3.matches("\\s*")) {
                    modify_line2.add(exp3.trim() + ";");
                    addLines.put(endLine, modify_line2);
                }

            }

        }
        if(!updateLines.isEmpty()) {
            fm.updateLinesToFile(whileFile, updateLines);
        }
        if(!addLines.isEmpty()) {
            fm.addLinesToFile(whileFile, addLines, true);
        }

        formatFile(whileFile);

        return whileFile;

    }

    public static File forExp1Lift(File file) {
        File liftFile = new File(file.getParent() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_lift.c");
        if (liftFile.exists()) {
            liftFile.delete();
        }
        try {
            liftFile.createNewFile();
            FileModify fm = new FileModify();
            fm.copyFile(liftFile, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Map<Integer, Integer> edge = new HashMap<Integer, Integer>();

        AstInform_Gen astgen = new AstInform_Gen(liftFile);
//        if (astgen.astList.size() == 0) return null;

        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);
        for (LoopStatement singleLoop : loopList) {
            edge.put(singleLoop.getStartLine(), singleLoop.getEndLine());
        }

        String regexFor = "\\bfor\\s*\\((.*);(.*);(.*?)\\)\\s*\\{";
        Pattern pFor = Pattern.compile(regexFor);
        Matcher mFor;

        Map<Integer, List<String>> updateLines = new HashMap<Integer, List<String>>();
        Map<Integer, List<String>> addLines = new HashMap<Integer, List<String>>();
        FileModify fm = new FileModify();
        List<String> initialList = fm.readFile(liftFile);
        for (Integer i : edge.keySet()) {
            String forhead = initialList.get(i - 1);
            mFor = pFor.matcher(forhead);
            if (mFor.find()) {
                //for( int i = 0; i < n; i++)
                List<String> modify_line1 = new ArrayList<String>();
                List<String> modify_line2 = new ArrayList<String>();
                String exp1 = mFor.group(1);

                if (!exp1.matches("\\s*")) {
                    modify_line1.add(forhead.replace(exp1, ""));
                    updateLines.put(i, modify_line1);
                    modify_line2.add(exp1.trim() + ";");
                    addLines.put(i, modify_line2);
                }
            }

        }
        if(!updateLines.isEmpty()) {
            fm.updateLinesToFile(liftFile, updateLines);
        }
        if(!addLines.isEmpty()) {
            fm.addLinesToFile(liftFile, addLines, true);
        }

        formatFile(liftFile);

        return liftFile;

    }

    public static boolean isHaveFor(File file){
        AstInform_Gen astgen = new AstInform_Gen(file);
        if(!file.exists()) {
            System.out.println(file.getAbsolutePath()+"exists exception! deleted!");
            return false;
        }
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> loopList = loopGen.outmostLoopList;
        if(loopList.size() != 0) {
            for(LoopStatement loop: loopList) {
                if(loop.getStmtType().equals("ForStmt")){
                    return true;
                }
            }
        }
        return false;
    }

    public static void formatFile(File file){
        String command = "clang-format -i " + file.getAbsolutePath();
        ProcessTerminal pt = new ProcessTerminal();
        pt.processThreadNotLimitJustExec(command, "sh");
    }
}
