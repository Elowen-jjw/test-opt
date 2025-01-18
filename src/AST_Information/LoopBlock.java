package AST_Information;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import AST_Information.model.LoopStatement;

public class LoopBlock {
	
	
	public static int getOutmostSline(LoopStatement stmt) {
		return getOutmostLoop(stmt).getStartLine();
	}

	public static int getOutmostEline(LoopStatement stmt) {
		return getOutmostLoop(stmt).getEndLine();
	}
	
	public static LoopStatement getOutmostLoop(LoopStatement stmt) {
		LoopStatement child = stmt;
		LoopStatement parent = stmt.getParentLoopStmt();
		while(parent != null) {
			child = parent;
			parent = child.getParentLoopStmt();
		}
		return child;
	}
	
	public static String getCondition(String type, String line) {
		String regexFor = "\\bfor\\s*\\((.*);(.*);(.*?)\\)\\s*\\{";
		String regexWhile = "\\bwhile\\s*\\((.*)\\)\\s*\\{";
		String regexDowhile = "\\bwhile\\s*\\((.*)\\)\\s*;";
		Pattern pFor = Pattern.compile(regexFor);
		Pattern pWhile = Pattern.compile(regexWhile);
		Pattern pDowhile = Pattern.compile(regexDowhile);
		Matcher mFor, mWhile, mDowhile;
		String condition = "";
		if(type.equals("ForStmt")) {
			mFor = pFor.matcher(line);
			if(mFor.find()) {
				condition = mFor.group(2);
			}
		}else if(type.equals("WhileStmt")) {
			mWhile = pWhile.matcher(line);
			if(mWhile.find()) {
				condition = mWhile.group(1);
			}
		}else {
			mDowhile = pDowhile.matcher(line);
			if(mDowhile.find()) {
				condition = mDowhile.group(1);
			}
		}
		condition = condition.trim();
		
		return condition;
	}
	
	
	public static String getSpace(String line) {
		String regexSpace = "^(\\s*)\\S";
		Pattern pspace = Pattern.compile(regexSpace);
		Matcher mspace = pspace.matcher(line);
		String space = "";
		if(mspace.find()) space = mspace.group(1);
		return space;
	}
	
}
