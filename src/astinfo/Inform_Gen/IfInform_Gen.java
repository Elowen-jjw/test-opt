package astinfo.Inform_Gen;

import astinfo.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class IfInform_Gen{
	
	public List<IfStatement> outmostIfList = new ArrayList<IfStatement>();
	
	public IfInform_Gen(AstInform_Gen astGen) {
		getIfInform(astGen);
	}
	
	public void getIfInform(AstInform_Gen astGen) {
		//astList
		String filepath = astGen.file.getAbsolutePath();
		Map<String, AstVariable> allVarsMap = astGen.allVarsMap;
		List<String> astList = astGen.astList;
		Stack<IfStatement> ifStack = new Stack<IfStatement>();
		String regexStartLine1 = "<line:([0-9]+)(:[0-9]+)";
		String regexStartLine2 = ".*[a-zA-Z]+\\s0x[a-z0-9]+\\s<" + filepath + ".*?:([0-9]+):[0-9]+(,\\s.*)?>.*"; 
		String regexStartLine3 = "<.*>\\sline:([0-9]+)(:[0-9]+)";
		String regexEndLine = "line:([0-9]+)(:[0-9]+>)";
		String regexVarDecl = "(Parm)?VarDecl\\s(0x[0-9a-z]+)\\s";
		String regexVarUse = "\\s(Parm)?Var\\s(0x[0-9a-z]+)\\s'([_a-zA-Z]+[_a-zA-Z0-9]*)'";
		String regexNewStmt = "(.*)([A-Z]{1}[a-z]+Stmt)(\\s)";
		Pattern pStartLine1= Pattern.compile(regexStartLine1);
		Pattern pStartLine2 = Pattern.compile(regexStartLine2);
		Pattern pStartLine3 = Pattern.compile(regexStartLine3);
		Pattern pEndLine = Pattern.compile(regexEndLine);
		Pattern pVarDecl = Pattern.compile(regexVarDecl);
		Pattern pVarUse = Pattern.compile(regexVarUse);
		Pattern pNewStmt = Pattern.compile(regexNewStmt);
		Matcher mStartLine1, mStartLine2, mStartLine3, mEndLine;
		Matcher mVarDecl, mVarUse, mNewStmt;
		int startLine = 0, endLine = 0, tempLine = 0;
		int astTempLine = 0;
		int topEndLine = 0;
		IfStatement topIfStmt = null;
		IfStatement popIfStmt = null;
		AstVariable outsideVar = null;
		AstVariable insideVar = null;
		AstVariable useVar = null;

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
			//弹出所有不是当前操作行外层的ifstmt
			while(!ifStack.isEmpty()) {	//考虑两层嵌套在同一行结束的情况，for(){...for(){}}
				topIfStmt = ifStack.peek();
				topEndLine = topIfStmt.getEndLine();
				if(tempLine <= topEndLine) {	//当前操作行是栈顶元素的内部，退出循环
					break;
				}else {
					//System.out.println("??? hasPopStmt  newline: " + tempLine);
				//当前操作行不属于栈顶元素内部，弹出栈顶元素
					popIfStmt = ifStack.pop();
					popIfStmt.setAstEndLine(astTempLine-1);
					popIfStmt.getIfList().sort(new SortIfList());
					if(ifStack.isEmpty()) {
						//弹出popStmt后，栈为空，说明popStmt是最外层For循环
						outmostIfList.add(popIfStmt);
					}
				}
			}
			
			//stack里存放的都是没有pop出来的，所以useVar和declVar都离topStackStmt最近
			//3.无论栈顶元素是否弹出，判断是否有declVar，将其放入最近的Loop中insideVarList中
			mVarDecl = pVarDecl.matcher(line);
			if(mVarDecl.find()) {
				String varid = mVarDecl.group(2);
				insideVar = allVarsMap.get(varid);
				if(!ifStack.isEmpty()) {
					topIfStmt = ifStack.peek();
					topIfStmt.getInsideList().add(insideVar);
				}
			}
			
			//4.无论栈顶元素是否弹出，判断是否有varUse
			mVarUse = pVarUse.matcher(line);
			if(mVarUse.find()) {
				String varid = mVarUse.group(2);
				useVar = allVarsMap.get(varid);
				
				//(1).栈非空，从栈顶if开始，层层遍历if的parentIf
				IfStatement childIf, parentIf;
				if(!ifStack.isEmpty()) {
					childIf = ifStack.peek();
					parentIf = childIf.getParentIfStmt();
					boolean isFindInsideList = false;
					//childIf的inVar包括useVar
					for(AstVariable inVar: childIf.getInsideList()) {
						if(inVar.getId().equals(varid)) {
							isFindInsideList = true;
							break;
						}
					}
					//childLoop的inVar不包括useVar
					while( parentIf != null ) {
						//varid是否出现在childIf的inside中
						//不是childIf的inside，那么一定是childIf的useVar，再判断是否是childIf的outVar
						if(isFindInsideList == false) {
							boolean isExsitUseVar = false;
							//usevarList
							for(AstVariable usevar: childIf.getUseVarList()) {
								if(usevar.getId().equals(varid)) {
									isExsitUseVar = true;
									break;
								}
							}
							if(isExsitUseVar == false) {
								childIf.getUseVarList().add(useVar);
							}
							
							//outsideVarList
							for(AstVariable inVar: parentIf.getInsideList()) {
								if(inVar.getId().equals(varid)) {
									boolean isExsitOutVar = false;
									for(AstVariable outVar: childIf.getOutsideList()) {
										if(outVar.getId().equals(varid)) {
											isExsitOutVar = true;
											break;
										}
									}
									if(isExsitOutVar == false) {
										outsideVar = useVar;
										//outsideVar.getUseLine().add(tempLine);
										childIf.getOutsideList().add(outsideVar);
									}
									isFindInsideList = true;
									break;
								}
							}
						}
						
						if(isFindInsideList == false ) {
							childIf = parentIf;
							parentIf = childIf.getParentIfStmt();
						}else {
							break;
						}
					}
					
					//parent为空还没找到var的insideList，说明当前childIf是最外层的ifloop，var是最外层if外声明的变量
					if( isFindInsideList == false ) {
						boolean isExsitUseVar = false;
						//usevarList
						for(AstVariable usevar: childIf.getUseVarList()) {
							if(usevar.getId().equals(varid)) {
								isExsitUseVar = true;
								break;
							}
						}
						if(isExsitUseVar == false) {
							//useVar.getUseLine().add(tempLine);
							childIf.getUseVarList().add(useVar);
						}
						
						//outsideVarList
						boolean isExsitOutVar = false;
						for(AstVariable outVar: childIf.getOutsideList()) {
							if(outVar.getId().equals(varid)) {
								isExsitOutVar = true;
								break;
							}
						}
						if(isExsitOutVar == false) {
							outsideVar = useVar;
							//outsideVar.getUseLine().add(tempLine);
							childIf.getOutsideList().add(outsideVar);
						}
					}
				}//在stack中找到首个if，判断usevar是否为outvar并放入相应outlist中
			}
			
			//5.是否有新的Stmt,有则压入栈，tempLine就是新的stmt的startLine，第1步已经更新了
			boolean flagNewIf = false;
			boolean has_else = false;
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
				//declstmt没有compoundStmt
				if(stmtType.equals("IfStmt")) {
					flagNewIf = true;
					if(line.matches("^.*IfStmt\\s.*<.*>.*\\shas_else\\s*$")) {
						has_else = true;
					}
				}
			}
		
			if(flagNewIf == true) {
				IfStatement newIfStmt = new IfStatement();
				newIfStmt.setStartLine(startLine);
				newIfStmt.setEndLine(endLine);
				newIfStmt.setHasElse(has_else);
				newIfStmt.setAstStartLine(astTempLine);
				if(!ifStack.isEmpty()) {
					topIfStmt = ifStack.peek();
					topIfStmt.getIfList().add(newIfStmt);
					newIfStmt.setParentIfStmt(topIfStmt);
				}
				ifStack.push(newIfStmt);
			}
			
		}

		//二、遍历完ast所有行，stmtStack出栈
		while(!ifStack.isEmpty()) {
			popIfStmt = ifStack.pop();
			/*更新popStmt的所有顺序*/
			popIfStmt.getIfList().sort(new SortIfList());
			/**/
			if(ifStack.isEmpty()) {
				outmostIfList.add(popIfStmt);
			}
		}
	}
	
	
	class SortIfList implements Comparator<IfStatement>{
		@Override
        public int compare(IfStatement stmt1, IfStatement stmt2) {
			int cnt1 = stmt1.getStartLine();
			int cnt2 = stmt2.getStartLine();
            return cnt1 - cnt2;    // 升序排列
        } 
	}
}

