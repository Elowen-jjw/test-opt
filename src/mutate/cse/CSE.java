package mutate.cse;

import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.VarInform;
import AST_Information.model.AstVariable;
import ObjectOperation.datatype.Data;
import ObjectOperation.datatype.RandomNumber;
import ObjectOperation.file.FileModify;
import ObjectOperation.list.CommonOperation;
import common.before.CommonMutate;
import common.CommonInfoFromFile;
import common.PropertiesInfo;
import common.SideEffort;
import utity.AvailableVariable;
import utity.SideEffortInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.*;

public class CSE {
    AstInform_Gen astgen;
    File file;
    List<String> initialFileList;
    Map<Integer, List<SideEffortInfo>> sevMap;
    int addFunctionLineNumber = 0;
    StringBuilder csFunc = new StringBuilder();
    String returnType = "";
    StringBuilder commonCall = new StringBuilder();

    int mutCount = 0;

    List<AvailableVariable> commonAvarList = new ArrayList<>();
    List<AvailableVariable> structUnionAvarList = new ArrayList<>();

    List<AstVariable> globalVarList = new ArrayList<>();
    public CSE(File file){
        CommonMutate cm = new CommonMutate();
        cm.deleteVolatile(file);
        this.file = file;
        this.initialFileList = CommonOperation.genInitialList(file);
        SideEffort se = new SideEffort(file);
        se.getSideEffortVar("lts");
        sevMap = se.sevMap;
        addFunctionLineNumber = CommonInfoFromFile.getFuncStartLine(this.initialFileList);
    }

    public void run(){
        if(sevMap.size() <= 2) return;
        getGlobalAvarList();
        processVariables();
    }

    public void getGlobalAvarList(){
        astgen = new AstInform_Gen(file);
        Map<String, AstVariable> varMap = astgen.allVarsMap;
        globalVarList = new ArrayList<>();
        for(String s: varMap.keySet()){
            if(varMap.get(s).getIsGlobal()){
                globalVarList.add(varMap.get(s));
            }
        }
        List<AvailableVariable> allAvarList = VarInform.getInitialAvailableVarList(globalVarList, astgen);
//        avarList = VarInform.removeStructAndUnionOverall(allAvarList);
        for(AvailableVariable avar: allAvarList) {
            if(!avar.getType().contains("union") && !avar.getType().contains("struct")){
                commonAvarList.add(avar);
//                System.out.println("1-----" + avar.getValue() + "  " + avar.getType());
            }else{
                structUnionAvarList.add(avar);
//                System.out.println("2-----" + avar.getValue() + "  " + avar.getType());
            }
        }
    }

    public void processVariables() {
        Random random = new Random();
        int tryCount1 = 0;
        while (commonAvarList.size() >= 4 && tryCount1++ < (commonAvarList.size() / 4) * 2) {
            Collections.shuffle(commonAvarList);
            List<AvailableVariable> selected = commonAvarList.subList(0, 4);
            if (generateCSE(selected)) {
                tryCount1 = 0;
                commonAvarList.removeAll(selected);
            }
        }
        if(commonAvarList.size() < 4 && !commonAvarList.isEmpty()) {
            int tryCount2 = 0;
            while (tryCount2++ < 5) {
                List<AvailableVariable> currentList = new ArrayList<>(commonAvarList);
                // When fewer than 4 items remain, repeat some to make up the numbers
                while (currentList.size() < 4) {
                    currentList.add(commonAvarList.get(random.nextInt(commonAvarList.size())));
                }
                if (generateCSE(currentList)) {
                    commonAvarList.clear();
                    break;
                }
            }
        }
        if(commonAvarList.isEmpty()){
            System.out.println("All variables have beed used!!!");
        } else {
            for(AvailableVariable av: commonAvarList){
                System.out.println(av.getValue());
            }
        }
    }

    public boolean generateCSE(List<AvailableVariable> selectedVarList) {
        //generate function call
        commonCall = new StringBuilder("cs(");
        StringBuilder declaration = new StringBuilder();

        List<String> dataTypeList = new ArrayList<>();
        List<String> paramList = new ArrayList<>();
        for (int i = 0; i < selectedVarList.size(); i++) {
            AvailableVariable av = selectedVarList.get(i);
            dataTypeList.add(av.getType());

            String combinedRegex = "(" + PropertiesInfo.notHaveBraceVarRegex + "|" + PropertiesInfo.haveBraceVarRegex + ")\\.f\\d+";
            Pattern p = Pattern.compile(combinedRegex);
            Matcher m = p.matcher(av.getValue());
            String callParamName = av.getValue();
            String declareParamType = av.getType();
            String fieldVar = "";//.f\\d+
            if(m.find()){
                callParamName = m.group(1);
                declareParamType = CommonInfoFromFile.getComplexType(callParamName, structUnionAvarList);
                fieldVar = av.getValue().substring(m.end(1)).trim();
                if(!CommonInfoFromFile.isBit(declareParamType, fieldVar.substring(1), astgen)){
                    callParamName = av.getValue();
                    declareParamType = av.getType();
                    fieldVar = "";
                }
            }
            int pointerLevel = getPointerLevel(callParamName);
            String pointerString = "*".repeat(pointerLevel);

            if(pointerLevel == 0) {
                declaration.append(String.format("%s%s, ", declareParamType + " *", PropertiesInfo.specificVarName.get(i)));
                commonCall.append(callParamName.matches(".*\\.f[0-9]+.*") ? String.format("&(%s), ", callParamName) : String.format("&%s, ", callParamName));
                paramList.add("(*" + PropertiesInfo.specificVarName.get(i) + ")" + fieldVar);
            }else{
                if(callParamName.matches(".*\\.f[0-9]+.*")){
                    declaration.append(String.format("%s%s, ", declareParamType + " *", PropertiesInfo.specificVarName.get(i)));
                    commonCall.append(String.format("&(%s), ", callParamName.replaceFirst("\\*", "").replaceAll("\\.(?=f\\d+)", "->")));
                    paramList.add("(*" + PropertiesInfo.specificVarName.get(i) + ")" + fieldVar);
                } else {
                    declaration.append(String.format("%s%s, ", declareParamType + " " + pointerString, PropertiesInfo.specificVarName.get(i)));
                    commonCall.append(String.format("%s, ", callParamName.replaceAll("\\*", "")));
                    paramList.add("(" + pointerString + PropertiesInfo.specificVarName.get(i) + ")"+ fieldVar);
                }
            }
        }

        returnType = Data.getMaxTypeInList(dataTypeList);
        declaration.append(returnType + " low, " + returnType + " high) {\n");

        System.out.println(paramList);
        //gen cs expression
        GenComplexExpr genExpr = new GenComplexExpr(paramList);
        List<String> expressionList = genExpr.genExpression(5);

        csFunc = new StringBuilder(returnType + " cs(" + declaration).append(getCsFuncBody(returnType, expressionList));

        //not generate the value of low and high
        return genCSEMutate();
    }

//    public boolean isBit(String complexType, String fieldName){
//        for(StructUnionBlock su: astgen.allStructUnionMap.values()) {
//            if((su.getBlockType() + " " + su.getName()).equals(complexType)){
//                for(FieldVar field: su.getChildField()) {
//                    if(field.getName().equals(fieldName))
//                        return field.getIsBit();
//                }
//            }
//        }
//        return false;
//    }
//
//    public String getComplexType(String varValue){
//        for(AvailableVariable avar: structUnionAvarList){
//            if(avar.getValue().equals(varValue))
//                return avar.getType();
//        }
//        return null;
//    }

    public int getPointerLevel(String value) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if(value.charAt(i) == '*') {
                count++;
                sb.append("*");
            }
        }
        return count;
    }

    public boolean genCSEMutate(){
        boolean isCorrect = false;
        File muDir = new File(PropertiesInfo.mutateCseDir + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")));
        if(mutCount == 0) {
            CommonInfoFromFile.cpOriginalFile(muDir, file);
        }

        File muFile = new File(muDir.getAbsolutePath() + "/" + file.getName().substring(0, file.getName().lastIndexOf(".c")) + "_" + (mutCount++) + ".c");
        try {
            if(muFile.exists()){
                muFile.delete();
            }
            muFile.createNewFile();

            FileWriter fw = new FileWriter(muFile, true);
            PrintWriter pw = new PrintWriter(fw);
            int count = 0;
            for(String line: initialFileList){
                count++;
                if(line.trim().equals("static long __undefined;")){
                    pw.println("#define CS_IN_RANGE(low, high, ...) (cs(__VA_ARGS__) >= (low) && cs(__VA_ARGS__) <= (high))");
                    pw.println(line);
                }
                else if(count == addFunctionLineNumber){
                    pw.println(csFunc.toString());
                    pw.println(line);
                }
                else if(sevMap.containsKey(count)){
                    String newLine = line;
                    for(SideEffortInfo sev: sevMap.get(count)) {
                        RandomNumber rn = new RandomNumber();
                        String low = rn.getRandomNum(returnType);
                        String high = rn.getRandomNumHasMin(returnType, low);
                        StringBuilder call = new StringBuilder(commonCall);
                        call.append(low + ", " + high + ")");
                        String judgeStatement = "CS_IN_RANGE(" + low + ", " + high + ", " + call.substring(3, call.lastIndexOf(")")) + ") ? ";
                        if(sev.getLeftValue().equals("")){
                            newLine = newLine.replace(sev.getStatement(), judgeStatement + sev.getStatement() + " : "
                                    + sev.getStatement().replaceAll("\\+\\+", "###temp###").replaceAll("--", "++").replaceAll("###temp###", "--"));
                        } else {
                            String[] type = sev.getRightType().split("/");
                            String deadValue = "";
                            Collections.shuffle(globalVarList);
                            Random random = new Random();
                            Boolean isUseAddress = random.nextBoolean();
                            for (AstVariable astVar : globalVarList) {
                                deadValue = sev.getLeftValue();
                                if (type.length == 1 || !isUseAddress) {
                                    if (astVar.getType().equals(type[0].trim())) {
                                        deadValue = astVar.getName();
                                        break;
                                    }
                                } else {
                                    if (astVar.getType().equals(type[1].replace("&", "").trim())) {
                                        deadValue = "&" + astVar.getName();
                                        break;
                                    }
                                }
                            }
//                            System.out.println(sev.getStatement());
                            newLine = newLine.replace(sev.getStatement(), sev.getLeftValue() + " " + sev.getOperator() + " "
                                    + judgeStatement + sev.getRightValue() + " : " + deadValue);

                        }
                    }
                    pw.println(newLine);
                }
                else{
                    pw.println(line);
                }
            }

            pw.flush();
            fw.flush();
            pw.close();
            fw.close();

            FileModify.formatFile(muFile);

            isCorrect = CommonInfoFromFile.checkCorrection(muFile);
            System.out.println(mutCount + " " + isCorrect);
            if(!isCorrect) {
                muFile.delete();
                mutCount--;
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return isCorrect;
    }

    public String getCsFuncBody(String dataType, List<String> expressionList){
        shuffle(expressionList);
        StringBuilder sb = new StringBuilder();
        sb.append(dataType + " temp = low - 1;\n");
        sb.append("int count = 0;\n");
        sb.append("while(count++ < 1000){\n");
        sb.append("if(temp < low || temp > high){\n");
        sb.append("int random = rand() % 5;\n");
        sb.append("switch(random){\n");
        sb.append("case 0:\n");
        sb.append("temp += " + expressionList.get(0) + ";\n");
        sb.append("break;\n");

        sb.append("case 1:\n");
        sb.append("temp -= " + expressionList.get(1) + ";\n");
        sb.append("break;\n");

        sb.append("case 2:\n");
        sb.append("temp |= " + expressionList.get(2) + ";\n");
        sb.append("break;\n");

        sb.append("case 3:\n");
        sb.append("temp ^= " + expressionList.get(3) + ";\n");
        sb.append("break;\n");

        sb.append("case 4:\n");
        sb.append("temp &= " + expressionList.get(4) + ";\n");
        sb.append("break;\n");

        sb.append("default:\n");
        sb.append("break;\n");
        sb.append("}\n");
        sb.append("}\n");
        sb.append("else{\n");
        sb.append("return temp;\n");
        sb.append("}\n");
        sb.append("}\n");
        sb.append("return low + count % (high - low);\n");
        sb.append("}\n");

        return sb.toString();
    }

}
