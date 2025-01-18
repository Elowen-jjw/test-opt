package astinfo.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class FunctionBlock {
	public String id;
	public String name;
	public int startline;
	public int endline;
	public int leftBraceLine;
	public int rightBraceLine;
	public boolean ifExtern = false;
	private List<String> wholeFunction;
	private List<String> functionBody;
	public TreeSet<Integer> returnLineSet = new TreeSet<Integer>();
	
	public List<String> getWholeFunction(List<String> initialList){
		wholeFunction = new ArrayList<String>();
		for(int i = startline; i <= endline; i++) {
			wholeFunction.add(initialList.get(i));
		}
		
		return wholeFunction;
	}
	public void setReturnLineSet(TreeSet<Integer> returnLineSet) {
		this.returnLineSet = returnLineSet;
	}
	public Set<Integer> getReturnLineSet(){
		return returnLineSet;
	}
	public List<String> getFunctioBody(List<String> initialList){
		functionBody = new ArrayList<String>();
		for(int i = leftBraceLine+1; i < endline; i++) {
			functionBody.add(initialList.get(i));
		}
		
		return functionBody;
	}
}
