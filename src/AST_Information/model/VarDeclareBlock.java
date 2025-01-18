package AST_Information.model;

import java.util.ArrayList;

public class VarDeclareBlock {
	protected String id;
	protected boolean isCinit = false;
	protected int startLine;
	protected int endLine;
	protected int astStartLine;
	protected int astEndLine;
	//等号左边的变量id
	protected String leftVar;
	//等号右边的变量id
	protected ArrayList<String> rightVar = new ArrayList<String>();
	protected String expression;
	protected String leftExp;
	protected String rightExp;
	
	public VarDeclareBlock(String id, int startLine, int endLine, int astStartLine, int astEndLine) {
		this.id = id;
		this.startLine = startLine;
		this.endLine = endLine;
		this.astStartLine = astStartLine;
		this.astEndLine = astEndLine;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	public String getId() {
		return id;
	}
	public void setIsCinit(boolean isCinit) {
		this.isCinit = isCinit;
	}
	public boolean getIsCinit() {
		return isCinit;
	}
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
	public void setLeftVar(String varid) {
		this.leftVar = varid;
	}
	public String getLeftVar() {
		return leftVar;
	}
	public void setRightVar(ArrayList<String> rightVar) {
		this.rightVar = rightVar;
	}
	public ArrayList<String> getRightVar(){
		return rightVar;
	}
	public void setExpression(String expression) {
		this.expression = expression;
	}
	public String getExpression() {
		return expression;
	}
	public void setLeftExp(String leftExp) {
		this.leftExp = leftExp;
	}
	public String getLeftExp() {
		return leftExp;
	}
	public void setRightExp(String rightExp) {
		this.rightExp = rightExp;
	}
	public String gerRightExp() {
		return rightExp;
	}
	
}
