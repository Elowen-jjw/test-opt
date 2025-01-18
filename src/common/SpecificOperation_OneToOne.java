package common;

import utity.*;

import java.util.ArrayList;
import java.util.List;

public class SpecificOperation_OneToOne {
    ProcessingBlock initialBlock;
    List<ProcessingBlock> transformedBlockList;
    List<AvailableVariable> avarList;
    List<InitialAndTransBlock> newItaList = new ArrayList<>();

    public boolean isTrans;

    public void transName(FixedStuff fs){
        avarList = fs.getAvarList();
        isTrans = true;

        for(InitialAndTransBlock ita: fs.getIatList()){
            while(!avarList.isEmpty() && isTrans) {
                InitialAndTransBlock newIta = new InitialAndTransBlock(ita);//backups

                initialBlock = newIta.getInitialBlock();
                transformedBlockList = newIta.getTransformedBlockList();

                Trans();
                newItaList.add(newIta);
            }
        }
        fs.setIatList(newItaList);
    }

    public void Trans(){
        if(isNotAvailable()){
            isTrans = false;
            return;
        }

        modifyInitial();
        for(ProcessingBlock singleTrans: transformedBlockList){
            modifyTransformed(singleTrans);
        }
    }
    
    public boolean isNotAvailable() {
    	return false;
    }

    public void modifyInitial(){
        //initialBlock.setBlockList();
    }


    public void modifyTransformed(ProcessingBlock singleTrans){
        //singleTrans.setBlockList();
    }


}
