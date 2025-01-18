package astinfo.Inform_Gen;

import astinfo.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WhileInform_Gen{
	
	public List<WhileStatement> outmostWhileList = new ArrayList<WhileStatement>();
	
	public WhileInform_Gen(AstInform_Gen astGen) {
		getWhileInform(astGen);
	}
	
	public void getWhileInform(AstInform_Gen astGen) {
		//astList
		String filepath = astGen.file.getAbsolutePath();
		Map<String, AstVariable> allVarsMap = astGen.allVarsMap;
		List<String> astList = astGen.astList;
		Stack<LoopStatement> astStmtStack = new Stack<LoopStatement>();
		Stack<WhileStatement> whileStack = new Stack<WhileStatement>();
//		String regexStartLine1 = "<.*>\\sline:([0-9]+)(:[0-9]+)";
//		//String regexStartLine2 = ".*[a-zA-Z]+\\s0x[a-z0-9]+\\s<" + filepath + ".*:([0-9]+):[0-9]+(,\\s.*)?>.*"; 
//		String regexStartLine2 = ".*[a-zA-Z]+\\s0x[a-z0-9]+\\s<" + filepath + ".*?:([0-9]+):[0-9]+(,\\s.*)?>.*"; 
//		String regexStartLine3 = "<line:([0-9]+)(:[0-9]+)";
		String regexStartLine1 = "<line:([0-9]+)(:[0-9]+)";
		String regexStartLine2 = ".*[a-zA-Z]+\\s0x[a-z0-9]+\\s<" + filepath + ".*?:([0-9]+):[0-9]+(,\\s.*)?>.*"; 
		String regexStartLine3 = "<.*>\\sline:([0-9]+)(:[0-9]+)";
		String regexEndLine = "line:([0-9]+)(:[0-9]+>)";
		String regexStartCol1 = "<line:[0-9]+:([0-9]+)";
		String regexStartCol2 = "<col:([0-9]+)";
		String regexEndCol1 = "line:[0-9]+:([0-9]+)>";
		String regexEndCol2 = "col:([0-9]+)>";
		
//		String regexVarDecl = "(Parm)?VarDecl\\s(0x[0-9a-z]+)\\s(.*prev\\s0x[0-9a-z]+\\s)?<.*>.*?(\\bused\\b)?\\s([_a-zA-Z]+[_a-zA-Z0-9]*)\\s'([_a-zA-Z]+[_a-zA-Z0-9,\\.\\s\\(\\)\\*\\[\\]]*)'"; 
		String regexVarDecl = "(Parm)?VarDecl\\s(0x[0-9a-z]+)\\s";
//		String regexVarUse = "\\s(Parm)?Var\\s(0x[0-9a-z]+)\\s'([_a-zA-Z]+[_a-zA-Z0-9]*)'\\s'([_a-zA-Z]+[_a-zA-Z0-9\\s\\(\\)\\*\\[\\]]*)'";	// Var id '变量名' '类型'
		String regexVarUse = "\\s(Parm)?Var\\s(0x[0-9a-z]+)\\s'([_a-zA-Z]+[_a-zA-Z0-9]*)'";
		String regexNewStmt = "(.*)([A-Z]{1}[a-z]+Stmt)(\\s)";
		//String regexFileStart = ".*FunctionDecl\\s0x[0-9a-z]+\\s<" + filepath + ".*>.*";
		Pattern pStartLine1= Pattern.compile(regexStartLine1);
		Pattern pStartLine2 = Pattern.compile(regexStartLine2);
		Pattern pStartLine3 = Pattern.compile(regexStartLine3);
		Pattern pEndLine = Pattern.compile(regexEndLine);
		Pattern pStartCol1 = Pattern.compile(regexStartCol1);
		Pattern pStartCol2 = Pattern.compile(regexStartCol2);
		Pattern pEndCol1 = Pattern.compile(regexEndCol1);
		Pattern pEndCol2 = Pattern.compile(regexEndCol2);
		Pattern pVarDecl = Pattern.compile(regexVarDecl);
		Pattern pVarUse = Pattern.compile(regexVarUse);
		Pattern pNewStmt = Pattern.compile(regexNewStmt);
		//Pattern pFileStart = Pattern.compile(regexFileStart);
		Matcher mStartLine1, mStartLine2, mStartLine3, mEndLine;
		Matcher mStartCol1, mStartCol2, mEndCol1, mEndCol2;
		Matcher mVarDecl, mVarUse, mNewStmt;
		int startLine = 0, endLine = 0, tempLine = 0;
		int topEndLine = 0;
		WhileStatement newStmt = null;
		WhileStatement topStmt = null;
		WhileStatement popStmt = null;
		AstVariable outsideVar = null;
		AstVariable insideVar = null;
		AstVariable useVar = null;
		LoopStatement newAstStmt = null;
		LoopStatement topAstStmt = null;
		
		//一、遍历ast所有行
		for(String line: astList) {
			//1.update tempLine；不用改变startline
			mStartLine1 = pStartLine1.matcher(line);
			mStartLine2 = pStartLine2.matcher(line);
			mStartLine3 = pStartLine3.matcher(line);
			if(mStartLine1.find()) {
				tempLine = Integer.parseInt(mStartLine1.group(1));
			}else if(mStartLine2.find()) {
				tempLine = Integer.parseInt(mStartLine2.group(1));
			}else if(mStartLine3.find()) {
				tempLine = Integer.parseInt(mStartLine3.group(1));
			}
			
			//2.根据tempLine判断栈顶元素是否弹出
			//弹出所有不是当前操作行外层的whilestmt
			while(!whileStack.isEmpty()) {	//考虑两层嵌套在同一行结束的情况，for(){...for(){}}
				topStmt = whileStack.peek();
				topEndLine = topStmt.getEndLine();
				if(tempLine <= topEndLine) {	//当前操作行是栈顶元素的内部，退出循环
					break;
				}else {
					//System.out.println("??? hasPopStmt  newline: " + tempLine);
				//当前操作行不属于栈顶元素内部，弹出栈顶元素
					popStmt = whileStack.pop();
					popStmt.getWhileList().sort(new SortByStartLine());
					/**/
					if(whileStack.isEmpty()) {
						//弹出popStmt后，栈为空，说明popStmt是最外层While循环
						outmostWhileList.add(popStmt);
					}
				}
			}
			
			while(!astStmtStack.isEmpty()) {
				topAstStmt = astStmtStack.peek();
				topEndLine = topAstStmt.getEndLine();
				if(tempLine <= topEndLine) {
					break;
				}else {
					astStmtStack.pop();
				}
			}
			
			//stack里存放的都是没有pop出来的，所以useVar和declVar都离topStackStmt最近
			//3.无论栈顶元素是否弹出，判断是否有declVar，将其放入最近的Loop中insideVarList中
			mVarDecl = pVarDecl.matcher(line);
			if(mVarDecl.find()) {
				String varid = mVarDecl.group(2);
				insideVar = allVarsMap.get(varid);
				if(!whileStack.isEmpty()) {
					topStmt = whileStack.peek();
					topStmt.getInsideList().add(insideVar);
				}
			}
			
			//4.无论栈顶元素是否弹出，判断是否有varUse
			mVarUse = pVarUse.matcher(line);
			if(mVarUse.find()) {
				String varid = mVarUse.group(2);
				useVar = allVarsMap.get(varid);
				WhileStatement childLoop, parentLoop;
				//System.out.println("***hasUseLine templine: " + tempLine + "\n");
				
				//栈非空，从栈顶for开始，层层遍历for的parentFor
				if(!whileStack.isEmpty()) {
					//System.out.println("VarUse: " + vartype + " " + varname);
					//System.out.println("startline: " + startLine + "; endeline: " + endLine + "； templine: " + tempLine);
					//System.out.println("line: " + line + "\n");
					childLoop = whileStack.peek();
					parentLoop = childLoop.getParentWhileStmt();
					boolean isFindInsideList = false;
					//childLoop的inVar包括useVar
					for(AstVariable inVar: childLoop.getInsideList()) {
						if(inVar.getId().equals(varid)) {
							isFindInsideList = true;
							break;
						}
					}
					//childLoop的inVar不包括useVar
					while( parentLoop != null ) {
						//varname是否出现在childLoop的inside中
						//不是childLoop的inside，那么一定是childLoop的useVar，在判断是否是childLoop的outVar
						if(isFindInsideList == false) {
							boolean isExsitUseVar = false;
							//usevarList
							for(AstVariable usevar: childLoop.getUseVarList()) {
								if(usevar.getId().equals(varid)) {
									isExsitUseVar = true;
									break;
								}
							}
							if(isExsitUseVar == false) {
								childLoop.getUseVarList().add(useVar);
							}
							
							//outsideVarList
							for(AstVariable inVar: parentLoop.getInsideList()) {
								if(inVar.getId().equals(varid)) {
									boolean isExsitOutVar = false;
									for(AstVariable outVar: childLoop.getOutsideList()) {
										if(outVar.getId().equals(varid)) {
											isExsitOutVar = true;
											break;
										}
									}
									if(isExsitOutVar == false) {
										outsideVar = useVar;
										//outsideVar.getUseLine().add(tempLine);
										childLoop.getOutsideList().add(outsideVar);
									}
									isFindInsideList = true;
									break;
								}
							}
						}
						
						if(isFindInsideList == false ) {
							childLoop = parentLoop;
							parentLoop = childLoop.getParentWhileStmt();
						}else {
							break;
						}
					}
					
					//parent为空还没找到var的insideList，说明当前childLoop是最外层的whileloop，var是最外层While外声明的变量
					if( isFindInsideList == false ) {
						boolean isExsitUseVar = false;
						//usevarList
						for(AstVariable usevar: childLoop.getUseVarList()) {
							if(usevar.getId().equals(varid)) {
								isExsitUseVar = true;
								break;
							}
						}
						if(isExsitUseVar == false) {
							//useVar.getUseLine().add(tempLine);
							childLoop.getUseVarList().add(useVar);
						}
						
						//outsideVarList
						boolean isExsitOutVar = false;
						for(AstVariable outVar: childLoop.getOutsideList()) {
							if(outVar.getId().equals(varid)) {
								isExsitOutVar = true;
								break;
							}
						}
						if(isExsitOutVar == false) {
							outsideVar = useVar;
							//outsideVar.getUseLine().add(tempLine);
							childLoop.getOutsideList().add(outsideVar);
						}
					}
				}//在stack中找到首个loop，判断usevar是否为outvar并放入相应outlist中
			}
			
			//5.是否有新的Stmt,有则压入栈，tempLine就是新的stmt的startLine，第1步已经更新了
			boolean flagNewWhile = false;
			boolean flagCompound = false;
			boolean flagNewAstStmt = false;
			String stmtType = null;
			mNewStmt = pNewStmt.matcher(line);
			if(mNewStmt.find()) {
				startLine = tempLine;
				mEndLine = pEndLine.matcher(line);
				if(mEndLine.find()) {
					endLine = Integer.parseInt(mEndLine.group(1));
				}else {
					endLine = tempLine;
				}
				
				stmtType = mNewStmt.group(2);
				if(stmtType.equals("CompoundStmt")) {
					flagCompound = true;
				}else {
					if(stmtType.equals("DeclStmt") == false)flagNewAstStmt = true;
					if(stmtType.equals("WhileStmt")) {
						flagNewWhile = true;
					}
				}
			}
			
			if(flagNewAstStmt == true) {
				newAstStmt = new LoopStatement();
				newAstStmt.setStartLine(startLine);
				newAstStmt.setEndLine(endLine);
				newAstStmt.setStmtType(stmtType);
				astStmtStack.add(newAstStmt);
			}
			
			if(flagCompound == true) {
				if(!astStmtStack.isEmpty()) {
					topAstStmt = astStmtStack.peek();
					if(topAstStmt.getStmtType().equals("WhileStmt")) {
						topStmt = whileStack.peek();
						topStmt.setHasBrace(true);
						topStmt.setBraceStartLine(startLine);
						topStmt.setBraceEndLine(endLine);
						mStartCol1 = pStartCol1.matcher(line);
						mStartCol2 = pStartCol2.matcher(line);
						mEndCol1 = pEndCol1.matcher(line);
						mEndCol2 = pEndCol2.matcher(line);
						if(mStartCol1.find()) {
							int col = Integer.parseInt(mStartCol1.group(1));
							topStmt.setBraceStartCol(col);
						}else if(mStartCol2.find()) {
							int col = Integer.parseInt(mStartCol2.group(1));
							topStmt.setBraceStartCol(col);
						}
						if(mEndCol1.find()) {
							int col = Integer.parseInt(mEndCol1.group(1));
							topStmt.setBraceEndCol(col);
						}else if(mEndCol2.find()) {
							int col = Integer.parseInt(mEndCol2.group(1));
							topStmt.setBraceEndCol(col);
						}
					}
				}
			}
			
			if(flagNewWhile == true) {
				newStmt = new WhileStatement();
				newStmt.setStartLine(startLine);
				newStmt.setEndLine(endLine);
				if(!whileStack.isEmpty()) {
					topStmt = whileStack.peek();
					topStmt.getWhileList().add(newStmt);
					newStmt.setParentWhileStmt(topStmt);
				}
				whileStack.push(newStmt);
			}
		}

		
		//二、遍历完ast所有行，stmtStack出栈
		while(!whileStack.isEmpty()) {
			popStmt = whileStack.pop();
			/*更新popStmt的所有顺序*/
			popStmt.getWhileList().sort(new SortByStartLine());
			/**/
			if(whileStack.isEmpty()) {
				outmostWhileList.add(popStmt);
			}
		}
	}
	
	private static class SortByStartLine implements Comparator<WhileStatement>{
		@Override
        public int compare(WhileStatement stmt1, WhileStatement stmt2) {
			int cnt1 = stmt1.getStartLine();
			int cnt2 = stmt2.getStartLine();
            return cnt1 - cnt2;    // 升序排列
        } 
	}
}

