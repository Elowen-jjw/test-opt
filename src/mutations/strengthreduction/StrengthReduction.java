package mutations.strengthreduction;

import astinfo.Inform_Gen.AstInform_Gen;
import astinfo.VarInform;
import astinfo.model.AstVariable;
import astinfo.model.LoopStatement;
import common.MuProcessException;
import objectoperation.datatype.Data;
import objectoperation.list.RandomAndCheck;
import utity.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StrengthReduction {
    File file;
    AstInform_Gen ast;
    public static int gIndex = 0;
    int startLine;
    int endLine;
    LoopStatement currentLoop;
    int loopExecTimes;
    List<AstVariable> filterUseVarList = new ArrayList<>();
    List<AvailableVariable> filterUseAvarList = new ArrayList<>();
    Map<Integer, Integer> insideLoopMap = new HashMap<>();
    List<BasicIV> loopBasicIVList = new ArrayList<>();
    int currentIndex;

    ProcessingBlock initialBlock;
    List<ProcessingBlock> transformedBlockList;
    List<AvailableVariable> avarList;
    List<InitialAndTransBlock> newItaList = new ArrayList<>();

    public boolean isTrans;
    List<AvailableVariable> twoVar;

    String type;
    String arrName;
    String dOp = "";

    public StrengthReduction(LoopStatement loop, int loopIndex, int loopExecTimes){
        this.currentLoop = loop;
        this.currentIndex = loopIndex;
        this.loopExecTimes = loopExecTimes;
    }

    public void strengthReduction(FixedStuff fs){
        ast = fs.getAst();
        file = fs.getInitialFile();
        startLine = fs.getStartLine();
        endLine = fs.getEndLine();

        avarList = fs.getAvarList();
        isTrans = true;

        getFilterUseVarList();
        getInsideLoopMap();

        findBasicIV(fs.getIatList().get(0).getInitialBlock().getBlockList());

        if(!isAvailableTrans()){
            isTrans = false;
            return;
        }

        for(InitialAndTransBlock ita: fs.getIatList()){
            while(avarList.size() >= 2) {
                RandomAndCheck rc = new RandomAndCheck();
                twoVar = rc.getRandomAvailableVarChange(avarList, 2);
                Trans(ita);
            }
        }
        if(newItaList.isEmpty()){
            isTrans = false;
            return;
        }
        fs.setIatList(newItaList);
    }

    public void Trans(InitialAndTransBlock ita){
        for(BasicIV bi: loopBasicIVList) {
            InitialAndTransBlock newIta = new InitialAndTransBlock(ita);//backups

            initialBlock = newIta.getInitialBlock();
            transformedBlockList = newIta.getTransformedBlockList();

            List<String> typeList = new ArrayList<>();
            typeList.add(twoVar.get(0).getType());
            typeList.add(twoVar.get(1).getType());
            typeList.add(bi.getType());
            type = Data.getMaxTypeInList(typeList);

            Random random = new Random();

            arrName = "g_a" + gIndex;
            gIndex++;

            dOp = random.nextBoolean() ? " + " : " - ";

            modifyInitial(bi);

            List<String> ubList = new ArrayList<>();
            ubList.add("overflow");
            if(MuProcessException.isHaveUB(file, startLine, endLine, initialBlock, ubList)){
                System.out.println("generate undefined overflow.....");
                avarList.add(twoVar.get(1));
                return;
            }

            for(ProcessingBlock singleTrans: transformedBlockList){
                modifyTransformed(singleTrans, bi);
            }

            newItaList.add(newIta);
        }
    }

    public boolean isAvailableTrans(){
        if(loopExecTimes == 0){
            return false;
        }
        if(loopBasicIVList.isEmpty()){
            return false;
        }
        return true;
    }

    public void modifyInitial(BasicIV bi){
        String index = "ii_" + currentIndex;
        List<String> globalList = new ArrayList<>();
        List<String> beforeHeaderList = new ArrayList<>();
        List<String> intoChecksumList = new ArrayList<>();

        addGlobal(globalList, type, arrName);
        addInitialBeforeHeader(beforeHeaderList, index);
        beforeHeaderList.add("//strength reduction");
        addIntoChecksum(intoChecksumList, type, arrName);

        initialBlock.setGlobalDeclare(globalList);
        initialBlock.setAddLineBoforeHeader(beforeHeaderList);
        initialBlock.setIntoChecksum(intoChecksumList);

        List<String> addInLoop = addInitialInLoop(arrName, index, twoVar.get(0).getValue(), bi.getName(), dOp, twoVar.get(1).getValue());
        initialBlock.getBlockList().addAll(bi.getAddLineNumber() - 1, addInLoop);
    }


    public void modifyTransformed(ProcessingBlock singleTrans, BasicIV bi){
        String index = "ii_" + currentIndex;
        String sName = "s_" + currentIndex;
        List<String> globalList = new ArrayList<>();
        List<String> beforeHeaderList = new ArrayList<>();
        List<String> intoChecksumList = new ArrayList<>();

        addGlobal(globalList, type, arrName);

        String exp1 = "";
        if(bi.getAddLineNumber() == 2){
            String regexFor = "\\bfor\\s*\\((.*);(.*);(.*?)\\)\\s*\\{";
            Pattern pFor = Pattern.compile(regexFor);
            Matcher mFor = pFor.matcher(singleTrans.getBlockList().get(0));
            if(mFor.find()){
                exp1 = mFor.group(1).trim();
                if(exp1.matches(bi.getName() + "\\s*=.+")){
                    beforeHeaderList.add(exp1 + ";");
                    singleTrans.getBlockList().set(0, "for (; " + mFor.group(2).trim() + "; " + mFor.group(3).trim() + "){");
                }
            }
        }

        addTransBeforeHeader(beforeHeaderList, index, type, sName, twoVar.get(0).getValue(), bi.getName(), dOp, twoVar.get(1).getValue());
        beforeHeaderList.add("//strength reduction");

        addIntoChecksum(intoChecksumList, type, arrName);

        singleTrans.setGlobalDeclare(globalList);
        singleTrans.setAddLineBoforeHeader(beforeHeaderList);
        singleTrans.setIntoChecksum(intoChecksumList);

        List<String> addInLoop = addTransInLoop(arrName, index, sName, twoVar.get(0).getValue(), bi.getOp(), bi.getNum());
        singleTrans.getBlockList().addAll(bi.getAddLineNumber() - 1, addInLoop);
    }

    public void addGlobal(List<String> globalList, String type, String arrayName){
        globalList.add(type + " " + arrayName + "[" + loopExecTimes + "];");
    }

    public void addInitialBeforeHeader(List<String> beforeHeaderList, String indexName){
        beforeHeaderList.add("int " + indexName + " = 0;");
    }

    public void addTransBeforeHeader(List<String> beforeHeaderList, String indexName,
                                     String sType, String sName, String cValue, String biName, String op, String dValue){
        beforeHeaderList.add(sType + " " + sName + " = " + cValue + " * " + biName + op + dValue + ";");
        beforeHeaderList.add("int " + indexName + " = 0;");
    }

    public List<String> addInitialInLoop(String arrarName, String indexName, String cValue, String biName, String op, String dValue){
        List<String> addInLoop = new ArrayList<>();
        addInLoop.add(arrarName + "[" + indexName + "] = " + cValue + " * " + biName + op + dValue + ";");
        addInLoop.add(indexName + "++;");
        return addInLoop;
    }

    public List<String> addTransInLoop(String arrarName, String indexName, String sName, String cValue, String biOp, int biNum){
        List<String> addInLoop = new ArrayList<>();
        addInLoop.add(arrarName + "[" + indexName + "] = " + sName + ";");
        addInLoop.add(sName + " = " + sName + " " + biOp + " " + cValue + " * " + biNum + ";");
        addInLoop.add(indexName + "++;");
        return addInLoop;
    }

    public void addIntoChecksum(List<String> intoChecksumList, String type, String arrayName){
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

    public void getFilterUseVarList(){
        List<AstVariable> insideVarList = currentLoop.getInsideList();
        for(AstVariable v: currentLoop.getUseVarList()){
            if(!insideVarList.contains(v)){
                filterUseVarList.add(v);
            }
        }
        filterUseAvarList = VarInform.getInitialAvailableVarList(filterUseVarList, ast);
    }

    public void getInsideLoopMap(){
        for(LoopStatement insideLoop: currentLoop.getLoopList()){
            insideLoopMap.put(insideLoop.getStartLine() - startLine + 1, insideLoop.getEndLine() - startLine + 1);
        }
    }

    public void findBasicIV(List<String> loopBlock){
        for(AvailableVariable v: filterUseAvarList) {
            String name = v.getValue();
            String type = v.getType();
            int startLine = 0;
            int endLine = 0;
            int count = 0;
            int num = 0;
            String op = "";
            int lineNumberInBlock = 0;
            boolean isBasic = false;

            String regexname = name.replaceAll("\\[", "\\\\[")
                    .replaceAll("\\(", "\\\\(")
                    .replaceAll("\\)", "\\\\)")
                    .replaceAll("\\*", "\\\\*")
                    .replaceAll("\\.", "\\\\.");

            String regex1 = "("+regexname+"\\s*=\\s*"+regexname+"\\s*(\\+|-)\\s*[0-9]+)|(\\s*"+regexname+"\\s*(\\+|-)=\\s*[0-9]+)";
            String regex2 = "("+regexname+"\\s*(\\+\\+|--))|((\\+\\+|--)\\s*"+regexname+")";
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
    }

}
