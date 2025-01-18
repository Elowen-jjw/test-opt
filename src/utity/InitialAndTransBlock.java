package utity;

import objectoperation.list.CommonOperation;

import java.util.ArrayList;
import java.util.List;

public class InitialAndTransBlock {
    private ProcessingBlock initialBlock;
    private List<ProcessingBlock> transformedBlockList;

    public InitialAndTransBlock() {
    }

    public InitialAndTransBlock(ProcessingBlock initialBlock) {
        this.initialBlock = initialBlock;
    }

    public InitialAndTransBlock(ProcessingBlock initialBlock, List<ProcessingBlock> transformedBlockList) {
        this.initialBlock = initialBlock;
        this.transformedBlockList = transformedBlockList;
    }



    public InitialAndTransBlock(InitialAndTransBlock oldIta){
        ProcessingBlock initialPb = new ProcessingBlock();
        List<String> initBlockList = new ArrayList<>();
        CommonOperation.copyStringList(initBlockList, oldIta.getInitialBlock().getBlockList());
        initialPb.setBlockList(initBlockList);

        List<ProcessingBlock> transPbList = new ArrayList<>();
        for(ProcessingBlock trans: oldIta.getTransformedBlockList()){
            ProcessingBlock transPb = new ProcessingBlock();
            List<String> transBlockList = new ArrayList<>();
            CommonOperation.copyStringList(transBlockList, trans.getBlockList());
            transPb.setBlockList(transBlockList);
            transPbList.add(transPb);
        }
        this.initialBlock = initialPb;
        this.transformedBlockList = transPbList;
    }

    public ProcessingBlock getInitialBlock() {
        return initialBlock;
    }

    public void setInitialBlock(ProcessingBlock initialBlock) {
        this.initialBlock = initialBlock;
    }

    public List<ProcessingBlock> getTransformedBlockList() {
        return transformedBlockList;
    }

    public void setTransformedBlockList(List<ProcessingBlock> transformedBlock) {
        this.transformedBlockList = transformedBlock;
    }
}
