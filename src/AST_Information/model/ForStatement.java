package AST_Information.model;

import java.util.ArrayList;
import java.util.List;

public class ForStatement {
	private int startLine;
	private int astStartLine;
	private int endLine;
	private boolean hasBrace = false;
	private int braceStartLine = -1;
	private int braceStartCol = -1;
	private int braceEndLine = -1;
	private int braceEndCol = -1;
	private ForStatement parentForStmt = null;
	private List<AstVariable> insideList = new ArrayList<AstVariable>();
	private List<AstVariable> outsideList = new ArrayList<AstVariable>();
	private List<AstVariable> useVarList = new ArrayList<AstVariable>();
	private List<ForStatement> forList = new ArrayList<ForStatement>();
	
	public ForStatement() {
		// TODO Auto-generated constructor stub
	}
	
	public ForStatement(int startLine, int endLine, int astStartLine) {
		this.startLine = startLine;
		this.endLine = endLine;
		this.astStartLine = astStartLine;
	}
	
	public void setStartLine(int startLine) {
		this.startLine = startLine;
	}
	public int getStartLine() {
		return startLine;
	}
	public void setAstStartLine(int astStartLine) {
		this.astStartLine = astStartLine;
	}
	public int getAstStartLine() {
		return astStartLine;
	}
	public void setEndLine(int endLine) {
		this.endLine = endLine;
	}
	public int getEndLine() {
		return endLine;
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
	public void setParentForStmt(ForStatement parentForStmt) {
		this.parentForStmt = parentForStmt;
	}
	public ForStatement getParentForStmt() {
		return parentForStmt;
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
	public void setForList(List<ForStatement> forList) {
		this.forList = forList;
	}
	public List<ForStatement> getForList() {
		return forList;
	}
	
}
