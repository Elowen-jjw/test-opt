package astinfo.Inform_Gen;

import astinfo.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LoopInform_Gen{
	
	public List<LoopStatement> outmostLoopList = new ArrayList<LoopStatement>();
	
	public LoopInform_Gen(AstInform_Gen astGen) {
		getLoopInform(astGen);
	}
	
	public void getLoopInform(AstInform_Gen astGen) {
		//astList
		String filepath = astGen.file.getAbsolutePath();
		Map<String, AstVariable> allVarsMap = astGen.allVarsMap;
		List<String> astList = astGen.astList;
		Stack<LoopStatement> astStmtStack = new Stack<LoopStatement>();
		Stack<LoopStatement> loopStack = new Stack<LoopStatement>();
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
		int astTempLine = 0;
		int topEndLine = 0;
		LoopStatement newStmt = null;
		LoopStatement topStmt = null;
		LoopStatement popStmt = null;
		AstVariable outsideVar = null;
		AstVariable insideVar = null;
		AstVariable useVar = null;
		LoopStatement newAstStmt = null;
		LoopStatement topAstStmt = null;

		//一、遍历ast所有行
		for(String line: astList) {
			astTempLine++;
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
			//弹出所有不是当前操作行外层的forstmt
			while(!loopStack.isEmpty()) {	//考虑两层嵌套在同一行结束的情况，for(){...for(){}}
				topStmt = loopStack.peek();
				topEndLine = topStmt.getEndLine();
				if(tempLine <= topEndLine) {	//当前操作行是栈顶元素的内部，退出循环
					break;
				}else {
					//System.out.println("??? hasPopStmt  newline: " + tempLine);
				//当前操作行不属于栈顶元素内部，弹出栈顶元素
					popStmt = loopStack.pop();
					popStmt.setAstEndLine(astTempLine-1);
					popStmt.getLoopList().sort(new SortByStartLine());
					/**/
					if(loopStack.isEmpty()) {
						//弹出popStmt后，栈为空，说明popStmt是最外层For循环
						outmostLoopList.add(popStmt);
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
				if(!loopStack.isEmpty()) {
					topStmt = loopStack.peek();
					topStmt.getInsideList().add(insideVar);
				}
			}
			
			//4.无论栈顶元素是否弹出，判断是否有varUse
			mVarUse = pVarUse.matcher(line);
			if(mVarUse.find()) {
				String varid = mVarUse.group(2);
				useVar = allVarsMap.get(varid);
				LoopStatement childLoop, parentLoop;
				//System.out.println("***hasUseLine templine: " + tempLine + "\n");
				
				//栈非空，从栈顶for开始，层层遍历for的parentFor
				if(!loopStack.isEmpty()) {
					//System.out.println("VarUse: " + vartype + " " + varname);
					//System.out.println("startline: " + startLine + "; endeline: " + endLine + "； templine: " + tempLine);
					//System.out.println("line: " + line + "\n");
					childLoop = loopStack.peek();
					parentLoop = childLoop.getParentLoopStmt();
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
							parentLoop = childLoop.getParentLoopStmt();
						}else {
							break;
						}
					}
					
					//parent为空还没找到var的insideList，说明当前childLoop是最外层的forloop，var是最外层For外声明的变量
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
//							System.out.println("useVar: " + useVar.getName());
//							System.out.println(childLoop.getStartLine() + " " + childLoop.getEndLine()
//								+ " ");
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
			boolean flagNewLoop = false;
			boolean flagCompound = false;
			boolean flagNewAstStmt = false;
			int startCol = -1, endCol = -1;
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
				mStartCol1 = pStartCol1.matcher(line);
				mStartCol2 = pStartCol2.matcher(line);
				mEndCol1 = pEndCol1.matcher(line);
				mEndCol2 = pEndCol2.matcher(line);
				if(mStartCol1.find()) {
					startCol = Integer.parseInt(mStartCol1.group(1));
				}else if(mStartCol2.find()) {
					startCol = Integer.parseInt(mStartCol2.group(1));
				}
				if(mEndCol1.find()) {
					endCol = Integer.parseInt(mEndCol1.group(1));
				}else if(mEndCol2.find()) {
					endCol = Integer.parseInt(mEndCol2.group(1));
				}

				stmtType = mNewStmt.group(2);
				if(stmtType.equals("CompoundStmt")) {
					flagCompound = true;
				}else {
					//declstmt没有compoundStmt
					if(stmtType.equals("DeclStmt") == false) flagNewAstStmt = true;
					if(stmtType.equals("ForStmt") || stmtType.equals("WhileStmt") || stmtType.equals("DoStmt")) {
						flagNewLoop = true;
					}
				}
			}
			
			
			if(flagCompound == true) {
				if(!astStmtStack.isEmpty()) {
					topAstStmt = astStmtStack.peek();
					String stmtType1 = topAstStmt.getStmtType();
					if(stmtType1.equals("ForStmt") || stmtType1.equals("WhileStmt") || stmtType1.equals("DoStmt")) {
						topStmt = loopStack.peek();
						topStmt.setHasBrace(true);
						topStmt.setBraceStartLine(startLine);
						topStmt.setBraceEndLine(endLine);
						topStmt.setBraceStartCol(startCol);
						topStmt.setBraceEndCol(endCol);
					}
				}
			}else {
				if(flagNewAstStmt == true) {
					newAstStmt = new LoopStatement();
					newAstStmt.setStartLine(startLine);
					newAstStmt.setEndLine(endLine);
					newAstStmt.setStmtType(stmtType);
					newAstStmt.setStartCol(startCol);
					newAstStmt.setEndCol(endCol);
					newAstStmt.setAstStartLine(astTempLine);
					astStmtStack.add(newAstStmt);
					if(flagNewLoop == true) {
						newStmt = newAstStmt;
						if(!loopStack.isEmpty()) {
							topStmt = loopStack.peek();
							topStmt.getLoopList().add(newStmt);
							newStmt.setParentLoopStmt(topStmt);
						}
						loopStack.push(newStmt);
					}
				}
			}
		}

		//二、遍历完ast所有行，stmtStack出栈
		while(!loopStack.isEmpty()) {
			popStmt = loopStack.pop();
			/*更新popStmt的所有顺序*/
			popStmt.getLoopList().sort(new SortByStartLine());
			/**/
			if(loopStack.isEmpty()) {
				outmostLoopList.add(popStmt);
			}
		}
	}
	
	private static class SortByStartLine implements Comparator<LoopStatement>{
		@Override
        public int compare(LoopStatement stmt1, LoopStatement stmt2) {
			int cnt1 = stmt1.getStartLine();
			int cnt2 = stmt2.getStartLine();
            return cnt1 - cnt2;    // 升序排列
        } 
	}
}

