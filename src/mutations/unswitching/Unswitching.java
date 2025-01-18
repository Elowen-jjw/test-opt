package mutations.unswitching;

import objectoperation.datatype.RandomOperator;
import objectoperation.list.RandomAndCheck;
import objectoperation.structure.FindInfoInLoop;
import utity.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Unswitching {
    final static Map<Integer, String[]> operators = new HashMap<>(){{
        put(0, new String[]{"!", "sizeof", "~"});
        put(1, new String[]{">", ">=", "<", "<=", "==", "!="});
        put(2, new String[]{"&", "|", "^"});
        put(3, new String[]{"&&", "||"});
    }};
    ProcessingBlock initialBlock;
    List<ProcessingBlock> transformedBlockList;
    List<AvailableVariable> avarList;
    List<InitialAndTransBlock> newItaList = new ArrayList<>();

    String newCondition;

    public boolean isTrans;

    public void unswitching(FixedStuff fs){
        avarList = fs.getAvarList();
        isTrans = true;

        for(InitialAndTransBlock ita: fs.getIatList()){
            while(!avarList.isEmpty() && isTrans) {
                newCondition = getCondition();
                unswitchingTrans(ita);
            }
        }
        if(newItaList.isEmpty()){
            isTrans = false;
            return;
        }
        fs.setIatList(newItaList);
    }

    public void unswitchingTrans(InitialAndTransBlock ita){
        FindInfoInLoop fil = new FindInfoInLoop();
        List<IfInLoop> ifListInitial = fil.findInfoInLoop(ita.getInitialBlock().getBlockList());
        if(ifListInitial.isEmpty()){
            isTrans = false;
            return;
        }

        List<List<IfInLoop>> ifListTransList = new ArrayList<>();

        for(ProcessingBlock singleTrans: ita.getTransformedBlockList()){
            List<IfInLoop> ifListSingleTrans = fil.findInfoInLoop(singleTrans.getBlockList());
            ifListTransList.add(ifListSingleTrans);
        }

        for(int i=0; i<ifListInitial.size(); i++){
            InitialAndTransBlock newIta = new InitialAndTransBlock(ita);//backups

            initialBlock = newIta.getInitialBlock();
            transformedBlockList = newIta.getTransformedBlockList();

            modifyInitial(ifListInitial.get(i).getStartLine(), newCondition);
            int count = 0;
            for(List<IfInLoop> singleTrans: ifListTransList){
                IfInLoop correspondingTrans = singleTrans.get(i);
                modifyTransformed(transformedBlockList.get(count), correspondingTrans, newCondition);
                count++;
            }
            newItaList.add(newIta);
        }
    }

    public void modifyInitial(int startLine, String newCondition){
        changeCondition(startLine, newCondition, initialBlock);

        List<String> includeLibList = new ArrayList<>();
        includeLibList.add("#include<math.h>");
        includeLibList.add("#include<stdlib.h>");
        initialBlock.setAddIncludeLib(includeLibList);

        initialBlock.getBlockList().add(0, "//mutations.unswitching initial:");
    }

    private void changeCondition(int startLine, String newCondition, ProcessingBlock initialBlock) {
        String start = initialBlock.getBlockList().get(startLine - 1);
        String s_con = "\\bif\\s*\\((.*)\\)";
        Pattern p_con = Pattern.compile(s_con);
        Matcher m_con = p_con.matcher(start);
        if(m_con.find()){
            String condition = m_con.group(1);
            initialBlock.getBlockList().set(startLine-1, start.replace(condition, newCondition));
        }
    }


    public String getCondition(){
        Random random = new Random();
        RandomAndCheck rc = new RandomAndCheck();
        int varCount;
        if(avarList.size() >= 5){
            varCount = random.nextInt(3, 6);
        }else{
            varCount = avarList.size();
        }
        List<AvailableVariable> chosenVar = rc.getRandomAvailableVarChange(avarList, varCount);

        if(chosenVar.size() == 1){
            return chosenVar.get(0).getValue();
        }

        List<String> opList = new ArrayList<>();
        List<Integer> chosenOptSequence = getRandomSequenceInOperator(chosenVar.size()-1);
        for(Integer i: chosenOptSequence){
            opList.add(operators.get(i)[random.nextInt(operators.get(i).length)]);
        }
        String firstOp = operators.get(0)[random.nextInt(operators.get(0).length)];
        int leftBrace = random.nextInt(0, chosenVar.size());
        int rightBrace = random.nextInt(leftBrace, chosenVar.size());
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<chosenVar.size(); i++){
            if(leftBrace == i){
                sb.append(firstOp).append("(");
            }
            sb.append(chosenVar.get(i).getValue());
            if(rightBrace == i){
                sb.append(")");
            }
            if(i < (chosenVar.size() - 1)){
                sb.append(" ").append(opList.get(i)).append(" ");
            }
        }
        System.out.println(chosenOptSequence);
        System.out.println(sb);
        return sb.toString();
    }

    public List<Integer> getRandomSequenceInOperator(int size){
        Random random = new Random();
        RandomOperator ro = new RandomOperator();
        if(size <= 3) {
            List<Integer> backups = new ArrayList<>(List.of(1, 2, 3));
            List<Integer> nums = new ArrayList<>();
            for(int i = 0; i < size; i++){
                int index = random.nextInt(backups.size());
                nums.add(backups.get(index));
                backups.remove(index);
            }
            ro.combination(nums);
            return ro.allCom.get(random.nextInt(ro.allCom.size()));
        }else{
            List<Integer> nums = new ArrayList<>();
            for(int i = 1; i <= 3; i++){
                nums.add(i);
            }
            ro.combination(nums);
            List<Integer> sequence = new ArrayList<>(ro.allCom.get(random.nextInt(ro.allCom.size())));
            sequence.add(random.nextInt(1, 4));
            return sequence;
        }
    }

    public void modifyTransformed(ProcessingBlock currentTransBlock, IfInLoop correspondingTrans, String newCondition){
        int startLine = correspondingTrans.getStartLine();
        int endLine = correspondingTrans.getEndLine();
        changeCondition(startLine, newCondition, currentTransBlock);
        List<String> beforeList = currentTransBlock.getBlockList();
        List<String> latestList = new ArrayList<String>();

        latestList.add("//mutations.unswitching trans:");

        List<String> includeLibList = new ArrayList<>();
        includeLibList.add("#include<math.h>");
        includeLibList.add("#include<stdlib.h>");
        currentTransBlock.setAddIncludeLib(includeLibList);

        //if
        for(int i=0; i<beforeList.size(); i++){
            if(i == 0){
                latestList.add(beforeList.get(startLine - 1).trim());
                latestList.add(beforeList.get(i));
            }
            else if(i == (startLine - 1)){
                latestList.add("{");
                latestList.addAll(correspondingTrans.getIfBody());
                latestList.add("}");
            }
            else if(i > (startLine - 1) && i < endLine){
            }
            else if(i == beforeList.size() - 1){
                latestList.add(beforeList.get(i));
                latestList.add("}");
            }
            else{
                latestList.add(beforeList.get(i));
            }
        }
        //else
        for(int i=0; i<beforeList.size(); i++){
            if(i == 0){
                latestList.add("else {");
                latestList.add(beforeList.get(i));
            }
            else if(i == (startLine - 1)){
                latestList.add("{");
                latestList.addAll(correspondingTrans.getElseBody());
                latestList.add("}");
            }
            else if(i > (startLine-1) && i < endLine){
            }
            else if(i == beforeList.size() - 1){
                latestList.add(beforeList.get(i));
                latestList.add("}");
            }
            else{
                latestList.add(beforeList.get(i));
            }
        }
        currentTransBlock.setBlockList(latestList);
    }


}
