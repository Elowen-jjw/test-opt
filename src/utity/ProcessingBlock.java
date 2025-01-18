package utity;

import java.util.List;

public class ProcessingBlock {
    private List<String> blockList;
    private List<String> addLineBoforeHeader;
    private List<String> addIncludeLib;
    private List<String> globalDeclare;
    private List<String> intoChecksum;

    public ProcessingBlock() {
    }

    public ProcessingBlock(List<String> blockList) {
        this.blockList = blockList;
    }

    public List<String> getBlockList() {
        return blockList;
    }

    public void setBlockList(List<String> blockList) {
        this.blockList = blockList;
    }

    public List<String> getAddIncludeLib() {
        return addIncludeLib;
    }

    public void setAddIncludeLib(List<String> addIncludeLib) {
        this.addIncludeLib = addIncludeLib;
    }


    public List<String> getAddLineBoforeHeader() {
        return addLineBoforeHeader;
    }

    public void setAddLineBoforeHeader(List<String> addLineBoforeHeader) {
        this.addLineBoforeHeader = addLineBoforeHeader;
    }

    public List<String> getGlobalDeclare() {
        return globalDeclare;
    }

    public void setGlobalDeclare(List<String> globalDeclare) {
        this.globalDeclare = globalDeclare;
    }

    public List<String> getIntoChecksum() {
        return intoChecksum;
    }

    public void setIntoChecksum(List<String> intoChecksum) {
        this.intoChecksum = intoChecksum;
    }

    public boolean isAvailableInBlockList(){
        return (this.blockList != null) && !this.blockList.isEmpty();
    }
    public boolean isAvailableInAddIncludeLib(){
        return (this.addIncludeLib != null) && !this.addIncludeLib.isEmpty();
    }

    public boolean isAvailableInAddLineBoforeHeader(){
        return (this.addLineBoforeHeader != null) && !this.addLineBoforeHeader.isEmpty();
    }

    public boolean isAvailableInGlobalDeclare(){
        return (this.globalDeclare != null) && !this.globalDeclare.isEmpty();
    }

    public boolean isAvailableInIntoChecksum(){
        return (this.intoChecksum != null) && !this.intoChecksum.isEmpty();
    }


}
