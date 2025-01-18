package astinfo.model;

import java.util.ArrayList;
import java.util.List;

public class LoopStatement extends AstStatement{
	
	private int startCol;
	private int endCol;
	private int astStartLine;
	private int astEndLine;
	private String stmtType;
	private boolean hasBrace = false;
	private int braceStartLine = -1;
	private int braceStartCol = -1;
	private int braceEndLine = -1;
	private int braceEndCol = -1;
	
	private LoopStatement parentLoopStmt = null;
	
	private List<LoopStatement> loopList = new ArrayList<LoopStatement>();
	private List<IfStatement> ifList = new ArrayList<IfStatement>();
	
	public LoopStatement() {
		// TODO Auto-generated constructor stub
	}
	
	public LoopStatement(int startLine, int endLine, int astStartLine) {
		this.startLine = startLine;
		this.endLine = endLine;
		this.astStartLine = astStartLine;
	}
	
	public void setStartCol(int startCol) {
		this.startCol = startCol;
	}
	public int getStartCol() {
		return startCol;
	}
	public void setEndCol(int endCol) {
		this.endCol = endCol;
	}
	public int getEndCol() {
		return endCol;
	}
	public void setAstStartLine(int astStartLine) {
		this.astStartLine = astStartLine;
	}
	public int getAstStartLine() {
		return astStartLine;
	}
	public void setAstEndLine(int astEndLine) {
		this.astEndLine = astEndLine;
	}
	public int getAstEndLine() {
		return astEndLine;
	}
	public void setStmtType(String stmtType) {
		this.stmtType = stmtType;
	}
	public String getStmtType() {
		return stmtType;
	}
	public void setHasBrace(boolean hasBrace) {
		this.hasBrace = hasBrace;
	}
	public boolean getHasBrace() {
		return hasBrace;
	}
	public void setBraceStartLine(int braceStartLine) {
		this.braceStartLine = braceStartLine;
	}
	public int getBraceStartLine() {
		return braceStartLine;
	}
	public void setBraceStartCol(int braceStartCol) {
		this.braceStartCol = braceStartCol;
	}
	public int getBraceStartCol() {
		return braceStartCol;
	}
	public void setBraceEndLine(int braceEndLine) {
		this.braceEndLine = braceEndLine;
	}
	public int getBraceEndLine() {
		return braceEndLine;
	}
	public void setBraceEndCol(int braceEndCol) {
		this.braceEndCol = braceEndCol;
	}
	public int getBraceEndCol() {
		return braceEndCol;
	}
	public void setParentLoopStmt(LoopStatement parentLoopStmt) {
		this.parentLoopStmt = parentLoopStmt;
	}
	public LoopStatement getParentLoopStmt() {
		return parentLoopStmt;
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
	public void setLoopList(List<LoopStatement> loopList) {
		this.loopList = loopList;
	}
	public List<LoopStatement> getLoopList() {
		return loopList;
	}
	public void setIfList(List<IfStatement> ifList) {
		this.ifList = ifList;
	}
	public List<IfStatement> getIfList(){
		return ifList;
	}
	
}
