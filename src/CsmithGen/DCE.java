package CsmithGen;


import AST_Information.AstStmtOperation;
import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.Inform_Gen.LoopInform_Gen;
import AST_Information.model.AstVariable;
import AST_Information.model.FunctionBlock;
import AST_Information.model.LoopStatement;
import common.ExceptionCheck;
import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import ObjectOperation.structure.FindIfInfo;
import common.CommonInfoFromFile;
import common.PropertiesInfo;
import common.SideEffort;
import processtimer.ProcessTerminal;
import utity.IfInfo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DCE {
    File file;
    String commandType;
    List<String> initialFileList;
    List<FunctionBlock> functionBlockList = new ArrayList<>();

    List<LoopStatement> loopList = new ArrayList<>();
    Map<Integer, Integer> loopMap = new HashMap<>();

    Map<Integer, IfInfo> ifMap = new HashMap<>();

    Set<Integer> allAddPrintLines = new HashSet<>();
    Set<Set<Integer>> blockSets = new HashSet<>();
    Set<Integer> executedLines = new HashSet<>();
    Set<Integer> unexecutedLines = new HashSet<>();

    Set<String> cannotDelVarName = new HashSet<>();
    Map<String, Integer> varDecInfo = new HashMap<>();

    int globalUnusedCheckCount = 0;

    public DCE(File file, String commandType){
        this.file = file;
        this.commandType = commandType;
        this.initialFileList = CommonOperation.genInitialList(file);
    }

    public boolean run(){
        deleteUnexecIfAndLoop();
        deleteUnused();
//        addGlobalVarTargetLocal();
        dealblankLine();
        return checkCorrection();
    }

    public void deleteUnused(){
        //deal unused variable and unused function, find it and delete it directly
        dealUnusedVarAndFunc();

        //deal the unused but set variable, if it used the safe_math function, remained it
        dealUnusedButSet();

        //delete the loop whose body is blank and header is like for(int i=0; i<[0-9]+; i++);
        //delete if-else whose body is blank and the condition is just a const
        dealUselessPart();
    }

    public void dealUnusedVarAndFunc(){
        globalUnusedCheckCount = 1;
        while(globalUnusedCheckCount == 1 || !isHaveUnused().isEmpty()) {
            addMainComment();
            addCommentFrontValidateVar();
            deleteUnusedPart();
            deleteCommentFrontValidateVar();
            deleteMainComment();
            dealError("undeclared", "common error");
            globalUnusedCheckCount++;
        }
    }

    //查看if和loop这两种结构的覆盖情况
    public void deleteUnexecIfAndLoop(){
        allAddPrintLines.clear();
        executedLines.clear();
        unexecutedLines.clear();
        blockSets.clear();

        addPrintf();
        executeFile();
        deleteUnexectedLines();
        deleteAddedPrintf();
    }

    public void addPrintf(){
        getAllPrintLines();
        Map<Integer, List<String>> addLines = new HashMap<>();
        for(int i = 0; i < initialFileList.size(); i++){
            if(allAddPrintLines.contains(i + 1) && !initialFileList.get(i).trim().startsWith("printf")){
                String addPrintf = "printf(\"" + (i + 1) + "\\n\"); ";
                List<String> addLine = new ArrayList<>();
                addLine.add(addPrintf + initialFileList.get(i));
                addLines.put(i + 1, addLine);
            }
        }
        //在if和loop block body的每一行添加printf,except headers
        FileModify fm = new FileModify();
        fm.updateLinesToFile(file, addLines);
        //一定不要format
    }

    public void executeFile(){
        String aoutFilename = file.getName().substring(0, file.getName().indexOf(".c"))
                .replaceAll("[a-zA-Z_]", "");

        String command = (commandType.equals("gcc")? "export LANGUAGE=en && export LANG=en_US.UTF-8 && ": "")
                + "cd " + file.getParent() + " && " + commandType + " " + file.getName()
                + " -w -lm -o " + aoutFilename;
        ProcessTerminal.voidNotMemCheck(command, "sh");

        File aoutFile = new File(file.getParent() + "/" + aoutFilename);
        if(!aoutFile.exists()){
            executedLines.add(0);
//            System.out.println("This file has compiler errors after adding printf flag........");//考虑compiler error的情况
            return;
        }

        command = "cd " + file.getParent() + " && " + "./" + aoutFilename;
        List<String> execLines = ProcessTerminal.listMemCheck(command, 10, "sh", false, true, new ArrayList<>(Arrays.asList(aoutFilename)));
        for(String s: execLines){
            if(s.trim().matches("\\d+")){
                executedLines.add(Integer.parseInt(s.trim()));
            }
        }
//        System.out.println(execLines);
    }

    public void deleteUnexectedLines(){
        initialFileList = CommonOperation.genInitialList(file);
        for(Integer i: allAddPrintLines){
            if(!executedLines.contains(i)){
                unexecutedLines.add(i);
            }
        }
        Set<Integer> deletedLineNumber = new HashSet<>();
        for(Set<Integer> block: blockSets){
            if(unexecutedLines.containsAll(block))
                deletedLineNumber.addAll(block);
        }
        unexecutedLines.removeAll(deletedLineNumber);
        for(Integer i: unexecutedLines){
            if(!isDeclareHavePrintf(initialFileList.get(i - 1))
                    && !initialFileList.get(i - 1).contains("}")
                    && !initialFileList.get(i - 1).contains("{")
                    && !initialFileList.get(i - 1).endsWith(":")){
                deletedLineNumber.add(i);
            }
        }
        FileModify fm = new FileModify();
        fm.deleteLinesToFile(file, deletedLineNumber);
        //一定不要format
    }

    public boolean isDeclareHavePrintf(String line){
        String initialLine = line.replaceAll("printf\\(\\\"[0-9]+\\\\n\\\"\\)", "").trim();
        if(initialLine.length() == 0) return false;
        String[] words = initialLine.split("\\s+");
        for(String word: words){
            if(PropertiesInfo.typeList.contains(word))
                return true;
        }
        return false;
    }

    public void deleteAddedPrintf(){
        initialFileList = CommonOperation.genInitialList(file);
        Map<Integer, List<String>> addLines = new HashMap<>();
        for(int i = 0; i < initialFileList.size(); i++) {
            String line = initialFileList.get(i).trim();
            Pattern p = Pattern.compile("(printf\\(\\\"[0-9]+\\\\n\\\"\\);).*");
            Matcher m = p.matcher(line.trim());
            if(m.find()){
//                System.out.println("----------------------------" + m.group());
                String addPrintf = m.group(1);
                List<String> addLine = new ArrayList<>();
                addLine.add(line.replace(addPrintf, ""));
                addLines.put(i + 1, addLine);
            }
        }
        FileModify fm = new FileModify();
        fm.updateLinesToFile(file, addLines);
        refreshFileList();
    }

    public void addMainComment(){
        boolean isStartMain = false;
        Map<Integer, List<String>> addLines = new HashMap<>();
        for(int i = 0; i < initialFileList.size(); i++) {
            String line = initialFileList.get(i).trim();
            if(line.equals("int main(void) {")){
                isStartMain = true;
                continue;
            }

            if(isStartMain && line.startsWith("transparent_crc(")){
                List<String> addLine = new ArrayList<>();
                addLine.add("//" + line);
                addLines.put(i + 1, addLine);
            }
        }
        FileModify fm = new FileModify();
        fm.updateLinesToFile(file, addLines);
        refreshFileList();
    }

    //对于gcc -Wunused命令来说，如果variable是validate类型，unused-variable check不出来，因此要先将validate注释掉
    public void addCommentFrontValidateVar(){
        Map<Integer, List<String>> addLines = new HashMap<>();
        for(int i = 0; i < initialFileList.size(); i++){
            String s = initialFileList.get(i).trim();
            if(s.contains("volatile ") && !s.startsWith("//")){
                List<String> addLine = new ArrayList<>();
                addLine.add("//" + s);
                addLine.add(s.replaceAll("volatile", ""));
                addLines.put(i + 1, addLine);
            }
        }
        FileModify fm = new FileModify();
        fm.updateLinesToFile(file, addLines);
        refreshFileList();
    }

    public void deleteUnusedPart(){
        int count = 0;
        Set<Integer> deleteNumberSet;
        while(!(deleteNumberSet = isHaveUnused()).isEmpty()){
//            System.out.println("Unused Deletion Iteration: " + ++count);
            FileModify fm = new FileModify();
            fm.deleteLinesToFile(file, deleteNumberSet);
            refreshFileList();
        }
    }

    public Set<Integer> isHaveUnused(){
        getFunctionList();

    	String command = getCommand("unused");
        List<String> execLines = getExecLines(command);

        Set<Integer> deleteNumberSet = new HashSet<>();
        for(String s: execLines){
            Pattern p;
            Matcher m;
            if(s.matches(".*[‘']__undefined[’'].*")) continue;
            if(s.contains("[-Wunused-variable]") || s.contains("[-Wunused-const-variable")){
                String regex = CommonInfoFromFile.replaceRegex(file.getName()) + ":([0-9]+):([0-9]+):\\s*warning:\\s*(.*)";
                p = Pattern.compile(regex);
                m = p.matcher(s.trim());
                if(m.find()){
                    int lineNumber = Integer.parseInt(m.group(1));
//                    if(m.group(3).contains("unused variable")){
//                        String indexPattern = ".*[‘']([ijkml][0-9]+)[’'].*";
//                        Pattern p_index = Pattern.compile(indexPattern);
//                        Matcher m_index = p_index.matcher(m.group(3));
//                        if(m_index.find()){
//                            String indexName = m_index.group(1);
//                            System.out.println(indexName);
//                            continue;
//                        }
//                    }
//                    System.out.println(lineNumber + " " + s);
                    if(initialFileList.get(lineNumber - 1).trim().matches("int\\s*[ijklmno](,\\s*[ijklmno])+\\s*;")) continue;
                    deleteNumberSet.add(lineNumber);
                }
            } else if(s.contains("[-Wunused-function]")){
//                System.out.println(globalUnusedCheckCount + " ::::: " + s);
                String regex = CommonInfoFromFile.replaceRegex(file.getName()) + ":([0-9]+):([0-9]+):\\s*warning:\\s*(.*)[‘'](func_[0-9]+)[’'](.*)";
                p = Pattern.compile(regex);
                m = p.matcher(s.trim());
                if(m.find()){
                    String funcName = m.group(4);
                    boolean isHaveFunc = false;
                    for(FunctionBlock fb: functionBlockList){
                        if(fb.name.equals(funcName)){
//                            System.out.println(fb.name + ": startLine: " + fb.startline + ", endline: " + fb.endline);
//                            CommonOperation.printList(CommonOperation.getListPart(initialFileList, fb.startline, fb.endline));
                            for(int i = fb.startline; i <= fb.endline; i++){
                                deleteNumberSet.add(i);
                            }
                            isHaveFunc = true;
                            break;
                        }
                    }
                    if(!isHaveFunc){
//                        System.out.println("function " + funcName + " has only declare........");
                        deleteNumberSet.add(Integer.parseInt(m.group(1)));
                    }
                }
            }
        }

        return deleteNumberSet;
    }

    public void deleteCommentFrontValidateVar(){
        Map<Integer, List<String>> addLines = new HashMap<>();
        Set<Integer> deleteNumberSet = new HashSet<>();
        for(int i = 0; i < initialFileList.size(); i++) {
            String s = initialFileList.get(i).trim();
            if(s.startsWith("//") && s.contains("volatile")){
                String nextLine = initialFileList.get(i+1).trim();
                if(s.substring(2).trim().replaceAll("volatile", "").replaceAll(" ", "")
                        .equals(nextLine.replaceAll(" ", ""))){
                    List<String> addLine = new ArrayList<>();
                    addLine.add(s.substring(2).trim());
                    addLines.put(i + 1, addLine);
                    deleteNumberSet.add(i + 2);
                }else{
                    deleteNumberSet.add(i + 1);
                }
            }
        }
        FileModify fm = new FileModify();
        fm.updateLinesToFile(file, addLines);
        fm.deleteLinesToFile(file, deleteNumberSet);
        refreshFileList();
    }

    public void deleteMainComment(){
        boolean isStartMain = false;
        Map<Integer, List<String>> addLines = new HashMap<>();
        for(int i = 0; i < initialFileList.size(); i++) {
            String line = initialFileList.get(i).trim();
            if(line.equals("int main(void) {")){
                isStartMain = true;
                continue;
            }
            if(isStartMain && line.startsWith("//") && line.substring(2).trim().startsWith("transparent_crc(")){
                List<String> addLine = new ArrayList<>();
                addLine.add(line.substring(2));
                addLines.put(i + 1, addLine);
            }
        }
        FileModify fm = new FileModify();
        fm.updateLinesToFile(file, addLines);
        refreshFileList();
    }

    public void dealError(String errorType, String commandErrorType){
        Set<Integer> deleteNumberSet;
        int count = 0;
        while(!(deleteNumberSet = isHaveError(errorType, commandErrorType)).isEmpty()){
//            System.out.println("Error Deletion Iteration: " + ++count);
            FileModify fm = new FileModify();
            fm.deleteLinesToFile(file, deleteNumberSet);
            refreshFileList();
        }
    }

    public Set<Integer> isHaveError(String errorType, String commandErrorType){
    	String command = getCommand(commandErrorType);
        List<String> execLines = getExecLines(command);
        Set<Integer> deleteNumberSet = new HashSet<>();
        for(String s: execLines){
            if(s.contains("error:") && s.contains(errorType)){
                String regex = CommonInfoFromFile.replaceRegex(file.getName()) + ":([0-9]+):([0-9]+):.*error:\\s*.*";
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(s.trim());
                if(m.find()){
                    if(initialFileList.get(Integer.parseInt(m.group(1)) - 1).trim().matches("int\\s*[ijklmno](,\\s*[ijklmno])+\\s*;")) continue;
                    deleteNumberSet.add(Integer.parseInt(m.group(1)));
                }
            }
        }
        return deleteNumberSet;
    }

    public void dealUnusedButSet(){
        int count1 = 0;
        while(isHaveUnusedButSetVar()){
//            System.out.println("unused but set variable check circle: " + ++count1 + ".........................");
            int count2 = 0;
            while(isHaveErrorUnusedButSet()){
//                System.out.println("unused but set variable deletion circle: " + ++count2 + ".........................");
//                System.out.println("\n");
            }
//            System.out.println("\n\n");
        }
        dealAddedComment();
    }

    public boolean isHaveUnusedButSetVar() {
        String command = getCommand("unused");

        //add comment in unused but set variable's declare line
        List<String> execUnusedLines = getExecLines(command);
        Map<Integer, List<String>> addComment = new HashMap<>();
        varDecInfo = new HashMap<>();
        for(String s: execUnusedLines){
            if(s.trim().matches(".*[‘']__undefined[’'].*")) continue;
            if(s.trim().contains("[-Wunused-but-set-variable]")){
                Pattern p;
                Matcher m;
                String regex = CommonInfoFromFile.replaceRegex(file.getName()) + ":([0-9]+):([0-9]+):\\s*warning:\\s*.*[‘']([lg]_[0-9]+)[’'].*";
                p = Pattern.compile(regex);
                m = p.matcher(s.trim());
                if(m.find()){
//                    System.out.println(s);
                    //only update one line and don't add the sum of line number
                    List<String> addLine = new ArrayList<>();
                    int lineNumber = Integer.parseInt(m.group(1));
                    String varName = m.group(3);
                    if(!cannotDelVarName.contains(varName)) {
//                        System.out.println(varName + " don't exists in the cannotDelVarNameSet.........................");
//                        System.out.println("varName: " + varName + ", lineNumber: " + lineNumber + ",   line: " + initialFileList.get(lineNumber - 1));
                        varDecInfo.put(varName, lineNumber);
                        addLine.add("//" + initialFileList.get(lineNumber - 1));
                        addComment.put(lineNumber, addLine);
                    }else{
//                        System.out.println(varName + " has already existed in the cannotDelVarNameSetCustomize.........................");
                    }
                }
            }
        }
        if(addComment.isEmpty()){
            return false;
        }

        FileModify fm = new FileModify();
        fm.updateLinesToFile(file, addComment);//don't change the sum of lines in file
        refreshFileList();

        return true;
    }

    public boolean isHaveErrorUnusedButSet(){
        String command = getCommand("common error");
        List<String> execErrorLines = getExecLines(command);

        Map<Integer, List<String>> deleteCommentAndLine = new HashMap<>();
        for(int i = 0; i < execErrorLines.size(); i++){
            String s = execErrorLines.get(i).trim();
            if(s.matches(".*[‘']__undefined[’'].*")) continue;
            if(s.contains("error:") && s.contains("undeclared")){
                String regex = CommonInfoFromFile.replaceRegex(file.getName()) + ":([0-9]+):([0-9]+):\\s*error:.*[‘']([lg]_[0-9]+)[’'].*";
                Pattern p = Pattern.compile(regex);
                Matcher m = p.matcher(s.replaceAll("did you mean [‘'][lg]_[0-9]+[’']", ""));
                if(m.find()){
                    int lineNumber = Integer.parseInt(m.group(1));
                    List<String> addLine = new ArrayList<>();
                    //If the assignment of this variable used the safe_math function, remains it.
                    if(execErrorLines.get(i + 1).contains("safe_") && execErrorLines.get(i + 1).contains("_func")
                           ) {// || countBrackets(execErrorLines.get(i + 1)) > 2
                        String varName = m.group(3);
                        cannotDelVarName.add(varName);
                        //delete comment that located in the front of var declare
                        if(varDecInfo.containsKey(varName) && initialFileList.get(varDecInfo.get(varName) - 1).trim().startsWith("//")){
//                            System.out.println("delete '//' in front of the var declare: " + initialFileList.get(varDecInfo.get(varName) - 1).trim().substring(2) + ".........................");
                            addLine.add(initialFileList.get(varDecInfo.get(varName) - 1).trim().substring(2));
                            deleteCommentAndLine.put(varDecInfo.get(varName), addLine);
                        }
                    }else {
//                        System.out.println("delete this set of var: " + execErrorLines.get(i + 1) + ".........................");
                        addLine.add("//"); //guarantee the sum of line number don't have change, cause if changed, the loop makes the varDecInfo invalid.
                        deleteCommentAndLine.put(lineNumber, addLine);
                    }
                }
            }
        }
        if(deleteCommentAndLine.isEmpty()){
            return false;
        }

        FileModify fm = new FileModify();
        fm.updateLinesToFile(file, deleteCommentAndLine);
        refreshFileList();
        return true;
    }

    public int countBrackets(String s){
        Pattern pattern = Pattern.compile("\\[[ijklmno][0-9]+\\]");
        Matcher matcher = pattern.matcher(s);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public void dealAddedComment(){
//        System.out.println("---------------start to delete add comment: ");
        Set<Integer> deleteNumberSet = new HashSet<>();
        int count = 0;
        for(String s: initialFileList) {
            count++;
            if(s.trim().startsWith("//")){
//                System.out.println(s);
                deleteNumberSet.add(count);
            }
        }
        FileModify fm = new FileModify();
        fm.deleteLinesToFile(file, deleteNumberSet);
        refreshFileList();
    }

    public void dealUselessPart(){
//        System.out.println("start to delete unused part:");
        while(isHaveBlankIfBody() || isHaveBlankLoopBody());
        deleteUnusedPart();
    }

    public boolean isHaveBlankLoopBody(){
        getLoopList();
        Set<Integer> deleteNumberSet = new HashSet<>();
        for(int i = 1; i <= initialFileList.size(); i++){
            if(loopMap.containsKey(i)){ //deal with blank loop body whose index is like ijklmno
                Pattern p = Pattern.compile("\\s*for\\s*\\(\\s*[ijklmno][0-9]*\\s*=\\s*[0-9]*\\s*;\\s*[ijklmno][0-9]*\\s*[<>]=?\\s*[0-9]+\\s*;\\s*[ijklmno][0-9]*\\s*(\\+\\+|--|[\\+-]=\\s*[0-9]+)\\s*\\)");
                Matcher m = p.matcher(initialFileList.get(i - 1));
                if(!m.find()) continue;
                int startLine = i;
                int endLine = loopMap.get(startLine);
                if(startLine + 1 == endLine && initialFileList.get(endLine - 1).trim().equals("}")){
                    deleteNumberSet.add(startLine);
                    deleteNumberSet.add(endLine);
                } else if(startLine == endLine){
                    deleteNumberSet.add(startLine);
                } else{
                    List<String> loopBody = CommonOperation.getListPart(initialFileList, startLine + 1, endLine - 1);
                    if(isBlankInList(loopBody)){
                        for(int j = startLine; j <= endLine; j++){
                            deleteNumberSet.add(j);
                        }
                    }
                }
            }
        }
        if(deleteNumberSet.isEmpty()){
            return false;
        }
        FileModify fm = new FileModify();
        fm.deleteLinesToFile(file, deleteNumberSet);
        refreshFileList();
        return true;
    }

    public boolean isHaveBlankIfBody(){
        try {
            getIfMap();
        }catch(Exception e){
            initialFileList.forEach(s -> System.out.println(s));
            e.printStackTrace();
            return false;
        }

        Map<Integer, List<String>> updateLines = new HashMap<>();
        Set<Integer> deleteNumberSet = new HashSet<>();
//        String simpleConditionRegex = "(0[xX][0-9a-fA-F]+U?[Ll]*)|([0-9]+U?[Ll]*)";
        String standard = "\\[(([0-9]*)|((\\*)*[glp]_[0-9]+(\\[(([0-9]*))\\])*(\\s*(->|\\.)\\s*f[0-9]+)?(\\[[0-9]*\\])*))\\]";
        String singleVarRegex = "\\(*((\\s*([~\\*])*[glp]_[0-9]+(" + standard + ")*(\\s*(->|\\.)\\s*f[0-9]+)?(" + standard + ")*)|((0[xX][0-9a-fA-F]+U?[Ll]*)|([0-9]+U?[Ll]*)))\\)*";

        for(int i = 1; i <= initialFileList.size(); i++) {
            if(ifMap.containsKey(i)){//deal with if-else whose body is blank
//                System.out.println("first: " + initialFileList.get(i-1) + ".................");
                IfInfo ii = ifMap.get(i);
                int startLine = i;
                String condition = ii.getCondition();
                int endLine = ii.getEndLine();
                int elseLine = ii.getElseLine();
                List<String> ifBody = ii.getIfBody();
                List<String> elseBody = ii.getElseBody();

                if(condition.matches(singleVarRegex) && isBlankInList(ifBody)){
//                    System.out.println("second: ...........");
                    if(elseLine == -1 || isBlankInList(elseBody)){
//                        System.out.println("third: ...........");
                        for(int j = startLine; j <= endLine; j++){
//                            System.out.println(j);
                            deleteNumberSet.add(j);
                        }
                    }
                } else if(elseLine != -1 && isBlankInList(elseBody)){
//                    System.out.println("five: .....................");
                    for(int j = elseLine + 1; j <= endLine; j++){
//                        System.out.println(j);
                        deleteNumberSet.add(j);
                    }
                    List<String> updateLine = new ArrayList<>();
                    updateLine.add(initialFileList.get(elseLine - 1).replaceAll("else\\s*\\{", ""));//只修改一行，修改之后不改变源程序的行
//                    System.out.println(elseLine + ": " + updateLine.get(0));
                    updateLines.put(elseLine, updateLine);
                }
            }
        }

        if(deleteNumberSet.isEmpty() && updateLines.isEmpty()){
            return false;
        }

        FileModify fm = new FileModify();
        fm.updateLinesToFile(file, updateLines);
        fm.deleteLinesToFile(file, deleteNumberSet);
        refreshFileList();
        return true;
    }

    public boolean isBlankInList(List<String> blockList){
        if(blockList.isEmpty()) return true;
        for(String s: blockList){
            if(!s.trim().equals("")){
                return false;
            }
        }
        return true;
    }


    public String getCommand(String checkType){
        String command = "";
        if(checkType.equals("unused")){
            if (commandType.equals("clang")){
                command = "cd " + file.getParent() + " && clang " + file.getName() + " -Wunused";
            }else if(commandType.equals("gcc")) {
                command = "export LANGUAGE=en && export LANG=en_US.UTF-8 && cd " + file.getParent() + " && gcc " + file.getName() + " -Wunused";
            }
        }
        else if(checkType.equals("common error")){
            if (commandType.equals("clang")){
                command = "cd " + file.getParent() + " && clang " + file.getName() + " -w -lm";
            }else if(commandType.equals("gcc")) {
                command = "export LANGUAGE=en && export LANG=en_US.UTF-8 && cd " + file.getParent() + " && gcc " + file.getName() + " -w -lm";
            }
        }
        else if(checkType.equals("sanitizer error")){
            if (commandType.equals("clang")){
                command = "cd " + file.getParent() + " && clang " + file.getName() + " -w -lm -fsanitize=undefined && ./a.out";
            }else if(commandType.equals("gcc")) {
                command = "export LANGUAGE=en && export LANG=en_US.UTF-8 && cd " + file.getParent() + " && gcc " + file.getName() + " -w -lm -fsanitize=undefined && ./a.out";
            }
        }
        return command;
    }

    public void getFunctionList(){
        AstInform_Gen astgen = new AstInform_Gen(file);
        functionBlockList = astgen.getAllFunctionBlocks();
    }

    public void getLoopList(){
        AstInform_Gen astgen = new AstInform_Gen(file);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        loopList = AstStmtOperation.getAllLoops(loopGen.outmostLoopList);
        loopMap.clear();
        for(LoopStatement loop: loopList){
            loopMap.put(loop.getStartLine(), loop.getEndLine());
        }
    }

    public void getIfMap(){
        ifMap.clear();
        FindIfInfo fii = new FindIfInfo();
        List<IfInfo> ifList = fii.findAllIfInfo(CommonOperation.genInitialList(file));
        for(IfInfo ii: ifList){
            ifMap.put(ii.getStartLine(), ii);
        }
    }

    public void getAllPrintLines(){
        getFunctionList();
        AstInform_Gen astgen = new AstInform_Gen(file);
        LoopInform_Gen loopGen = new LoopInform_Gen(astgen);
        List<LoopStatement> outermostLoopList = loopGen.outmostLoopList;
        List<LoopStatement> allLoopList = AstStmtOperation.getAllLoops(outermostLoopList);

        FindIfInfo fii = new FindIfInfo();
//        List<IfInfo> outermostIfList = fii.findOutermostIfInfo(CommonOperation.genInitialList(file));
        List<IfInfo> allIfList = fii.findAllIfInfo(CommonOperation.genInitialList(file));

        for(FunctionBlock fb: functionBlockList){
            for(int i = fb.startline + 1; i < fb.endline; i++){
                allAddPrintLines.add(i);
            }
        }

        for(LoopStatement loop: allLoopList){
            Set<Integer> loopSet = new HashSet<>();
            for(int i = loop.getStartLine(); i <= loop.getEndLine(); i++){
                loopSet.add(i);
            }
            blockSets.add(loopSet);

            Set<Integer> loopBodySet = new HashSet<>();
            for(int i = loop.getStartLine() + 1; i < loop.getEndLine(); i++){
                loopBodySet.add(i);
            }
            blockSets.add(loopBodySet);
        }


        for(IfInfo fi: allIfList){
            Set<Integer> ifSet = new HashSet<>();
            for(int i = fi.getStartLine(); i <= fi.getEndLine(); i++){
                ifSet.add(i);
            }
            blockSets.add(ifSet);

            Set<Integer> ifBodySet = new HashSet<>();
            for(int i = fi.getStartLine() + 1; i < fi.getElseLine(); i++){
                ifBodySet.add(i);
            }
            blockSets.add(ifBodySet);

            if(fi.getElseLine() != -1 && fi.getElseLine() + 1 != fi.getEndLine()) {
                Set<Integer> elseBodySet = new HashSet<>();
                for (int i = fi.getElseLine() + 1; i < fi.getEndLine(); i++) {
                    elseBodySet.add(i);
                }
                blockSets.add(elseBodySet);
            }
        }

    }

    private List<String> getExecLines(String command) {
        List<String> execLines = ProcessTerminal.listNotMemCheck(command, "sh");
        deleteAoutFile(file, "a.out");
        return execLines;
    }

    public void deleteAoutFile(File file, String aoutFilename){
        File outFile = new File(file.getParent() + "/" + aoutFilename);
        if(outFile.exists()){
            outFile.delete();
        }
    }



    public void refreshFileList(){
        FileModify.formatFile(file);
        initialFileList = CommonOperation.genInitialList(file);
    }

    public boolean checkCorrection(){
        ExceptionCheck ec = new ExceptionCheck();
        return ec.filterUB(file);
    }

    public void dealblankLine(){
        getFunctionList();
        Set<Integer> checkLines = new HashSet<>();
        for(FunctionBlock fb: functionBlockList){
            for(int i = fb.startline + 1; i < fb.endline; i++){
                checkLines.add(i);
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(fos);
            int count = 0;
            for(String s: initialFileList){
                count++;
                if(checkLines.contains(count) && s.trim().equals("")) continue;
                pw.println(s);
            }
            pw.flush();
            pw.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void addGlobalVarTargetLocal(){
        AstInform_Gen astgen = new AstInform_Gen(file);
        Map<String, AstVariable> varMap = astgen.allVarsMap;
        List<AstVariable> localVarList = new ArrayList<>();
        for(String s: varMap.keySet()){
//            boolean isNeedOperate = true;
            if(varMap.get(s).getName().matches("l_[0-9]+") && !varMap.get(s).getType().contains("*")) {
//                for(String line: initialFileList){
//                    if(line.contains("printf") && line.contains("checksum") && Arrays.stream(line.split("\\s+")).anyMatch(ss -> ss.contains(varMap.get(s).getName()))){
//                        isNeedOperate = false;
//                        break;
//                    }
//                }
//                if(isNeedOperate) {
                    localVarList.add(varMap.get(s));
//                    System.out.println(varMap.get(s).getName() + ": " + varMap.get(s).getType());
//                }
            }
        }

//        String regex1 = "(\\s*(l_[0-9]+)(\\[\\w*\\])*)\\s*(\\+\\+|--|[\\*\\+\\-\\%\\/&\\|\\^]?=|[<>]{2}=)(?!=)";
//        String regex2 = "([+]{2}|-{2})\\s*(\\s*(l_[0-9]+)(\\[\\w*\\])*)";
        Map<Integer, List<String>> addLines = new HashMap<>();
        Map<Integer, List<String>> updateLines = new HashMap<>();
        int globalDecLine = 0;
        int addPrintfInMainLine = 0;
        List<String> globalList = new ArrayList<>();
        List<String> printfList = new ArrayList<>();
        int count = 0;
        for(String s: initialFileList){
            count++;
            if(s.trim().matches("(\\s*\\w+\\s+)+func_1\\s*\\(\\s*(?:\\w+\\s*(?:\\*+\\s*)?)*\\);"))
                globalDecLine = count - 1;
            else if(s.trim().equals("func_1();"))
                addPrintfInMainLine = count + 1;
        }

        for(AstVariable av: localVarList){
            String varName = av.getName();
            String regex1 = "(\\s*(" + CommonInfoFromFile.replaceRegex(varName) + ")(\\[\\w*\\])*)\\s*(\\+\\+|--|[\\*\\+\\-\\%\\/&\\|\\^]?=|[<>]{2}=)(?!=)";
            String regex2 = "([+]{2}|-{2})\\s*((" + CommonInfoFromFile.replaceRegex(varName) + ")(\\[\\w*\\])*)";
            Pattern p1 = Pattern.compile(regex1);
            Pattern p2 = Pattern.compile(regex2);
            Matcher m1;
            Matcher m2;
            for(String s: initialFileList){
                if (SideEffort.isDeclareLine(s.trim())) continue;
                m1 = p1.matcher(s.trim());
                m2 = p2.matcher(s.trim());
                if (m1.find() || m2.find()) {
                    String varType = av.getType();
                    Pattern p_local = Pattern.compile("(\\w+)((\\[\\d+\\])*)");
                    Matcher m_local = p_local.matcher(varType);
                    if (m_local.find()) {
                        //update original declare if the var has side effort, don't change the sum of line number
                        //move declare from inside function to the global
                        //add printf in main
                        if (m_local.group(2).isEmpty()) {
//                            if(!av.getIsGlobal()) {
                                List<String> updateList = new ArrayList<>();
                                updateList.add(initialFileList.get(av.getDeclareLine() - 1).replace(m_local.group(1), ""));
                                updateLines.put(av.getDeclareLine(), updateList);

                                globalList.add("static " + m_local.group(1) + " " + varName + ";");
//                            }
                            printfList.add(String.format("printf(\"checksum %s = %s\\n\", %s);", varName, PropertiesInfo.typeToSpecifier.get(varType), varName));
                        } else {
//                            if(!av.getIsGlobal()) {
                                List<String> updateList = new ArrayList<>();
                                updateList.add("");
                                updateLines.put(av.getDeclareLine(), updateList);

                                globalList.add("static " + m_local.group(1) + " " + varName + m_local.group(2) + ";");
//                            }
                            printfList.add(getArrayPrintf(m_local.group(1), m_local.group(2), varName));
                        }
                        break;
                    }
                }
            }
        }

        addLines.put(globalDecLine, globalList);
        addLines.put(addPrintfInMainLine, printfList);

        FileModify fm = new FileModify();
        fm.updateLinesToFile(file, updateLines);
        fm.addLinesToFile(file, addLines, true);
        refreshFileList();
    }

    public String getArrayPrintf(String baseType, String dimensions, String varName){
        // 提取各维大小
        Matcher dimMatcher = Pattern.compile("\\[(\\d+)\\]").matcher(dimensions);
        int[] sizes = new int[6]; // 最多6维
        int dimCount = 0;
        while (dimMatcher.find() && dimCount < 6) {
            sizes[dimCount++] = Integer.parseInt(dimMatcher.group(1));
        }
        // 生成对应的C语言风格的printf输出代码
        StringBuilder sb = new StringBuilder();
        String[] indices = {"i", "j", "k", "l", "m", "n"};

        // 构建嵌套循环
        for (int i = 0; i < dimCount; i++) {
            sb.append("for (int ").append(indices[i]).append(" = 0; ").append(indices[i])
                    .append(" < ").append(sizes[i]).append("; ").append(indices[i]).append("++) {\n");
        }

        // 构建打印语句
        sb.append("    printf(\"checksum ").append(varName);
        for (int i = 0; i < dimCount; i++) {
            sb.append("[%d]");
        }
        sb.append(" = ").append(PropertiesInfo.typeToSpecifier.get(baseType)).append("\\n\", ");
        for (int i = 0; i < dimCount; i++) {
            sb.append(indices[i]).append(", ");
        }
        sb.append(varName);
        for (int i = 0; i < dimCount; i++) {
            sb.append("[").append(indices[i]).append("]");
        }
        sb.append(");\n");

        // 关闭循环
        for (int i = 0; i < dimCount; i++) {
            sb.append("}\n");
        }
        return sb.toString();
    }
}
