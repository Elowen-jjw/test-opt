package mutations.unswitching;

import objectoperation.list.RandomAndCheck;
import objectoperation.structure.FindInfoInLoop;
import utity.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnswitchingTwoInvariant {
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
            while(avarList.size() >= 2 && isTrans) {
                newCondition = getTwo();
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

//            CommonOperation.printList(initialBlock.getBlockList());
//            System.out.println("\n\n");
//            int number = 0;
//            for(ProcessingBlock singleTrans: transformedBlockList) {
//                System.out.println("Mutation " + ++number + "\n");
//                CommonOperation.printList(singleTrans.getBlockList());
//            }
//            System.out.println("\n\n");
        }

    }

    public void modifyInitial(int startLine, String newCondition){
        changeCondition(startLine, newCondition, initialBlock);

        List<String> includeLibList = new ArrayList<>();
        includeLibList.add("#include<math.h>");
        includeLibList.add("#include<stdlib.h>");
        initialBlock.setAddIncludeLib(includeLibList);

        initialBlock.getBlockList().add(0, "//mutations.unswitching two invaraint initial:");
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

    public String getTwo(){
        RandomAndCheck rc = new RandomAndCheck();
        List<AvailableVariable> chosenVar = rc.getRandomAvailableVarChange(avarList, 2);
//        MathFunction mf = new MathFunction();
//        return mf.getFucTwoVar() + "(" + chosenVar.get(0).getValue() + ", " + chosenVar.get(1).getValue() + ")";
        return "(" + chosenVar.get(0).getValue() + ", " + chosenVar.get(1).getValue() + ")";
    }

    public void modifyTransformed(ProcessingBlock currentTransBlock, IfInLoop correspondingTrans, String newCondition){
        int startLine = correspondingTrans.getStartLine();
        int endLine = correspondingTrans.getEndLine();
        changeCondition(startLine, newCondition, currentTransBlock);
        List<String> beforeList = currentTransBlock.getBlockList();
        List<String> latestList = new ArrayList<String>();

        latestList.add("//mutations.unswitching two invaraint trans:");

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
