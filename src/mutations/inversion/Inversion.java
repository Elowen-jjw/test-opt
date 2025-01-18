package mutations.inversion;

import utity.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Inversion {
    List<String> initialBlockList;
    List<ProcessingBlock> transformedBlockList;
    List<InitialAndTransBlock> newItaList = new ArrayList<>();

    public boolean isTrans;

    public void inversion(FixedStuff fs){
        isTrans = true;
        for(InitialAndTransBlock ita: fs.getIatList()){
            InitialAndTransBlock newIta = new InitialAndTransBlock(ita);//backups
            initialBlockList = newIta.getInitialBlock().getBlockList();
            transformedBlockList = newIta.getTransformedBlockList();
            Trans();
            newItaList.add(newIta);
        }
        if(newItaList.isEmpty()){
            isTrans = false;
            return;
        }
        fs.setIatList(newItaList);
    }

    public void Trans(){
        modifyInitial();
        for(ProcessingBlock singleTrans: transformedBlockList){
            modifyTransformed(singleTrans);
        }
    }

    public void modifyInitial(){
        initialBlockList.add(0, "//mutations.inversion initial: ");
    }


    public void modifyTransformed(ProcessingBlock singleTrans){
        List<String> whileBlock = singleTrans.getBlockList();
        List<String> dowhileBlock = whileBlockTrans(whileBlock);
        singleTrans.setBlockList(dowhileBlock);
    }

    public List<String> whileBlockTrans(List<String> l) {
        List<String> transBlock = new ArrayList<>();
        String first = l.get(0);
        String conditionLine = "";
        String regex = "\\bwhile\\s*\\(.+\\)";// while()
        Pattern p = Pattern.compile(regex);
        Matcher matcher = p.matcher(first);

        transBlock.add("//mutations.inversion trans:");

        if (matcher.find()) {
            conditionLine = matcher.group();
            transBlock.add(conditionLine.replace("while", "if") + "{");
            transBlock.add(first.replace(conditionLine, "do"));
        }

        for(int i=1; i<l.size()-1; i++) {
            transBlock.add(l.get(i));
        }

        String last = l.get(l.size() - 1);
        transBlock.add(last + conditionLine + ";");
        transBlock.add("}");

        return transBlock;
    }

}
