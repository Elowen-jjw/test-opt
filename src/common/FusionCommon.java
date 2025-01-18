package common;

import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.VarInform;
import astinfo.model.AstVariable;
import astinfo.model.LoopStatement;
import objectoperation.datatype.Data;
import objectoperation.datatype.RandomOperator;
import objectoperation.list.RandomAndCheck;
import utity.AvailableVariable;
import utity.BasicIV;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FusionCommon {

    public static List<AvailableVariable> getAVarUseList(AstInform_Gen ast, File file, LoopStatement loop){
        List<AstVariable> filterUseList = new ArrayList<>();
        for(AstVariable av: loop.getUseVarList()){
            System.out.println(av.getName() + ": " + av.getIsIntialized());
            if(av.getIsIntialized()){
                filterUseList.add(av);
            }
        }
        List<AvailableVariable> var_value_type = VarInform.getInitialAvailableVarList(filterUseList, ast);
        System.out.println( "before randomAndcheck: " + var_value_type.size());
        RandomAndCheck rc = new RandomAndCheck();
        List<AvailableVariable> avarUseList = rc.getAvailableVarList(file, var_value_type, loop.getStartLine() + 1);
        return avarUseList;
    }

    public static String getArrType(List<AvailableVariable> avarList, String type){
        List<String> typeList = new ArrayList<>();
        for(AvailableVariable av: avarList){
            typeList.add(av.getType());
        }
        typeList.add(type);
        return Data.getMaxTypeInList(typeList);
    }

    public static List<String> getValues(List<AvailableVariable> avarList){
        List<String> valueList = new ArrayList<>();
        for(AvailableVariable av: avarList){
            valueList.add(av.getValue());
        }
        return valueList;
    }

    public static List<AstVariable> getFilterUseVarList(LoopStatement currentLoop) {
        List<AstVariable> filterUseVarList = new ArrayList<>();
        List<AstVariable> insideVarList = currentLoop.getInsideList();
        for(AstVariable v: currentLoop.getUseVarList()){
            if(!insideVarList.contains(v)){
                filterUseVarList.add(v);
            }
        }
        return filterUseVarList;
    }

    public static Map<Integer, Integer> getInsideLoopMap(LoopStatement currentLoop){
        Map<Integer, Integer> insideLoopMap = new HashMap<>();
        for(LoopStatement insideLoop: currentLoop.getLoopList()){
            insideLoopMap.put(insideLoop.getStartLine() - currentLoop.getStartLine() + 1, insideLoop.getEndLine() - currentLoop.getStartLine() + 1);
        }
        return insideLoopMap;
    }

    public static List<BasicIV> findBasicIV(List<String> loopBlock, List<AstVariable> filterUseVarList,
                            Map<Integer, Integer> insideLoopMap){
        List<BasicIV> loopBasicIVList = new ArrayList<>();
        for(AstVariable v: filterUseVarList) {
            String name = v.getName();
            String type = v.getType();
            int startLine = 0;
            int endLine = 0;
            int count = 0;
            int num = 0;
            String op = "";
            int lineNumberInBlock = 0;
            boolean isBasic = false;

            String regex1 = "("+name+"\\s*=\\s*"+name+"\\s*(\\+|-)\\s*[0-9]+)|(\\s*"+name+"\\s*(\\+|-)=\\s*[0-9]+)";
            String regex2 = "("+name+"\\s*(\\+\\+|--))|((\\+\\+|--)\\s*"+name+")";
            Pattern p1 = Pattern.compile(regex1);
            Pattern p2 = Pattern.compile(regex2);

            for(String s: loopBlock) {
                count++;
                if(insideLoopMap.containsKey(count)){
                    startLine = count;
                    endLine = insideLoopMap.get(startLine);
                }
                if(count >= startLine && count <= endLine){
                    continue;
                }

                Matcher matcher1 = p1.matcher(s);
                Matcher matcher2 = p2.matcher(s);
                if(matcher1.find()) {
                    lineNumberInBlock = count;
                    isBasic = true;
                    String group = matcher1.group();
                    if(group.contains("+")) {
                        op = "+";
                    }
                    else if(group.contains("-")) {
                        op = "-";
                    }
                    String str = "";
                    String deleteNameGroup = group.replace(name, "");
                    for(int i=0; i<deleteNameGroup.length(); i++) {
                        if(deleteNameGroup.charAt(i) >= 48 && deleteNameGroup.charAt(i) <= 57) {
                            str += deleteNameGroup.charAt(i);
                        }
                    }
                    num = Integer.parseInt(str);
                    break;
                }
                else if(matcher2.find()) {
                    isBasic = true;
                    lineNumberInBlock = count;
                    num = 1;
                    String group = matcher2.group();
                    if(group.contains("+")) {
                        op = "+";
                    }
                    else if(group.contains("-")) {
                        op = "-";
                    }
                    break;
                }
            }
            if(isBasic) {
                int addNumber = 0;//add line in the front of which line in block
                if(lineNumberInBlock == 1)
                    addNumber = 2;
                else{
                    addNumber = lineNumberInBlock;
                }
                BasicIV bi = new BasicIV(name, type, num, op, lineNumberInBlock, addNumber);
                loopBasicIVList.add(bi);
            }
        }
        return loopBasicIVList;
    }

    public static void addGlobal(List<String> globalList, String type, String arrayName, int loopExecTimes){
        globalList.add(type + " " + arrayName + "[" + loopExecTimes + "];");
    }

    public static void addBeforeHeader(List<String> beforeHeaderList, String indexName){
        beforeHeaderList.add("int " + indexName + ";");
    }

    public static void addCommentBeforeHeader(List<String> beforeHeaderList, String comment){
        beforeHeaderList.add(comment);
    }


    public static void initFusionOperators(Map<Integer, String[]> fusionOperators){
        fusionOperators.clear();
        fusionOperators.put(0, new String[]{"!", "sizeof", "~"});
        fusionOperators.put(1, new String[]{"+", "-", "/", "%"});
        fusionOperators.put(2, new String[]{"*"});
        fusionOperators.put(3, new String[]{"<<", ">>"});
        fusionOperators.put(4, new String[]{">", ">=", "<", "<=", "==", "!="});
        fusionOperators.put(5, new String[]{"&", "|", "^"});
        fusionOperators.put(6, new String[]{"&&", "||"});
    }

    public static List<String> addBodyInLoop(String arrarName, String indexName, String cValue, String useVarName, String op, String dValue){
        List<String> addInLoop = new ArrayList<>();
        addInLoop.add(arrarName + "[" + indexName + "] = " + cValue + " * " + useVarName + op + dValue + ";");
        return addInLoop;
    }

    public static void addIntoChecksum(List<String> intoChecksumList, String type, String arrayName, int loopExecTimes){
        String dataType = "";
        if(type.equals("char") || type.equals("signed char") || type.equals("int8_t")
                || type.equals("short") || type.equals("signed short") || type.equals("signed short int") || type.equals("short int") || type.equals("int16_t")
                || type.equals("int") || type.equals("signed int") || type.equals("signed") || type.equals("int32_t")){
            dataType = "d";
        }else if(type.equals("unsigned char") || type.equals("uint8_t")
                || type.equals("unsigned short") || type.equals("unsigned short int") || type.equals("uint16_t")
                || type.equals("unsigned int") || type.equals("unsigned") || type.equals("uint32_t")){
            dataType = "u";
        }else if(type.equals("long") || type.equals("signed long") || type.equals("long int")
                || type.equals("signed long int")|| type.equals("int64_t")){
            dataType = "ld";
        }else if(type.equals("unsigned long") || type.equals("unsigned long int") || type.equals("uint64_t")){
            dataType = "lu";
        }
        intoChecksumList.add("for (i = 0; i < " + loopExecTimes + "; i++) {\n" +
                "printf(\"checksum " + arrayName + "[%d] = %" + dataType + "\\n\", i, " + arrayName + "[i]);\n" +
                "}");
    }

    public static List<AvailableVariable> getRandomAvarList(List<AvailableVariable> avarList){
        Random random = new Random();
        RandomAndCheck rc = new RandomAndCheck();
        int varCount = random.nextInt(1, Integer.min(avarList.size(), 4) + 1);
        return rc.getRandomAvailableVarChange(avarList, varCount);
    }

    public static List<String> addRandomBodyInLoop(String arrarName, String indexName, List<String> avarList, List<String> useVarList, Map<Integer, String[]> operators){
        Random random = new Random();
        List<String> addInLoop = new ArrayList<>();

        Set<String> varSet = new HashSet<>();
        varSet.addAll(avarList);
        varSet.addAll(useVarList);
        List<Integer> chosenOptSequence = getRandomSequenceInOperator(varSet.size() - 1);
        List<String> opList = new ArrayList<>();
        for(Integer i: chosenOptSequence){
            opList.add(operators.get(i)[random.nextInt(operators.get(i).length)]);
        }

        String firstOp = operators.get(0)[random.nextInt(operators.get(0).length)];
        int leftBrace = random.nextInt(0, varSet.size());
        int rightBrace = random.nextInt(leftBrace, varSet.size());
        StringBuilder sb = new StringBuilder();

        int i = 0;
        boolean isHaveDiv = false;
        for(String var: varSet){
            if(leftBrace == i){
                sb.append(firstOp).append("(");
            }
            if(isHaveDiv){
                sb.append("(").append(var).append("? ").append(var).append(": ").append(random.nextInt(0, Integer.MAX_VALUE) + 1).append(")");
                isHaveDiv = false;
            }else {
                sb.append(var);
            }
            if(rightBrace == i){
                sb.append(")");
            }
            if(i < (varSet.size() - 1)){
                sb.append(" ").append(opList.get(i)).append(" ");
                if(opList.get(i).equals("/") || opList.get(i).equals("%")){
                    isHaveDiv = true;
                }
            }
            i++;
        }
        System.out.println("varSet.size():" + varSet.size());
        System.out.println(chosenOptSequence);
        addInLoop.add(arrarName + "[" + indexName + "] = " + sb + ";");
        System.out.println(addInLoop.get(0));
        return addInLoop;
    }

    public static List<Integer> getRandomSequenceInOperator(int size){
        List<Integer> backups = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        Random random = new Random();
        RandomOperator ro = new RandomOperator();
        List<Integer> nums = new ArrayList<>();
        for(int i = 0; i < size; i++){
            int index = random.nextInt(backups.size());
            nums.add(backups.get(index));
            backups.remove(index);
        }
        ro.combination(nums);
        return ro.allCom.get(random.nextInt(ro.allCom.size()));
    }
}
