package astinfo.model;

import java.util.ArrayList;
import java.util.List;

public class AstStatement {
	protected String stmttype;
	protected int startLine;
	protected int endLine;
	protected List<AstVariable> insideList = new ArrayList<AstVariable>();
	protected List<AstVariable> outsideList = new ArrayList<AstVariable>();
	protected List<AstVariable> useVarList = new ArrayList<AstVariable>();
	
	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}
	public int getStartLine() {
		return startLine;
	}
	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}
	public int getEndLine() {
		return endLine;
	}
	public void setInsideList(List<AstVariable> insideList) {
		this.insideList = insideList;
	}
	public List<AstVariable> getInsideList() {
		return insideList;
	}
	public void setOutsideList(List<AstVariable> outsideList) {
		this.outsideList = outsideList;
	}
	public List<AstVariable> getOutsideList() {
		return outsideList;
	}
	public void setUseVarList(List<AstVariable> useVarList) {
		this.useVarList = useVarList;
	}
	public List<AstVariable> getUseVarList(){
		return useVarList;
	}
}
