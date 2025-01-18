package astinfo.Inform_Gen;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
import java.util.Stack;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import astinfo.model.*;
import processmemory.ProcessStatus;
import processmemory.ProcessWorker;

public class AstInform_Gen {
	
	public static void main(String args[]) {
		File file = new File("/home/jing/Desktop/csmith-testsuite/random13.c");
		AstInform_Gen ast = new AstInform_Gen(file);
		for(AstVariable var: ast.getAllVars()) {
			if(var.getIsGlobal() == true) {
				System.out.println(var.getKind() + " " + var.getDeclareLine() + " " + var.getType() + " " + var.getName());
			}
		}
		for(StructUnionBlock su: ast.allStructUnionMap.values()) {
			System.out.println(su.getBlockType() + " " + su.getName());
			for(FieldVar field: su.getChildField()) {
				System.out.println(field.getName() + " " + field.getType());
			}
			System.out.println();
		}
	}
	
	public File file;
	public Map<String, AstVariable> allVarsMap = new LinkedHashMap<String, AstVariable>();
	public Map<String, StructUnionBlock> allStructUnionMap = new HashMap<String, StructUnionBlock>();	//<id, block>
	public Map<String, FieldVar> allFieldVarsMap = new HashMap<String, FieldVar>();
	public Map<TypedefDecl, String> typedefDeclMap = new HashMap<TypedefDecl, String>(); //<typedefDecl, Recordid>
	
	public List<String> astList = new ArrayList<String>();
	//file行号，使用的变量id
	public Map<Integer, ArrayList<String>> lineUseMap = new HashMap<Integer, ArrayList<String>>();
	//file行号，定义的变量id
	public Map<Integer, ArrayList<String>> lineDeclMap = new HashMap<Integer, ArrayList<String>>();
	public Map<Integer, ArrayList<EqualOperatorBlock>> equalOpMap = new HashMap<Integer, ArrayList<EqualOperatorBlock>>();
	public List<EqualOperatorBlock> equalOpBlocks = new ArrayList<EqualOperatorBlock>();
	public Map<String, VarDeclareBlock> varDeclBlockMap = new HashMap<String, VarDeclareBlock>();	//<varid, vardeclareBlcok>
	public List<VarDeclareBlock> varDeclBlocks = new ArrayList<VarDeclareBlock>();
//	private boolean isStart = false;
	
	
	public AstInform_Gen(File file) {
		this.file = file;
		genAstList();
		genAstInform();
	} 
	
	public void genAstList(){
		String command = "clang -fsyntax-only -Xclang -ast-dump " + file.getAbsolutePath() + " -w -Xanalyzer -analyzer-disable-all-checking -I $CSMITH_HOME/include";
		
		try {
			List<String> execLines = testProcessThread(command);
			if(execLines.size() == 1 && execLines.get(0).trim().equals("timeout")) return ;
			
			boolean isStart = false;
			for(String line: execLines) {
				if(isStart == false) {
					//astList从这行的下一行开始：static long __undefined;
					//|-VarDecl 0x8ff0028 </home/jing/Desktop/csmith-testsuite/random3.c:13:1, col:13> col:13 __undefined 'long' static
					if(line.matches(".*\\bVarDecl\\s0x[a-z0-9]+\\s<.*>\\s.*\\s__undefined\\s'long'\\sstatic\\s*")) {
						isStart = true; 
					}
					continue ;
				}
				//遇见int main() 结束
//				FunctionDecl 0x9920790 <line:68:1, line:79:1> line:68:5 main 'int (void)'
				if(line.matches(".*\\bFunctionDecl\\s0x[a-z0-9]+\\s<.*>\\sline:[0-9]+:[0-9]+\\smain\\s'int\\s\\(void\\)'\\s*")) {
					break ;
				}
				
				astList.add(line);	//前提：ast没有error和warning
//				astListAddLine(line);
			}
			
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void genAstInform() {
		
		try {

			String regexStartLine1 = "<line:([0-9]+)(:[0-9]+)";
			String regexStartLine2 = ".*[a-zA-Z]+\\s0x[a-z0-9]+\\s<" + file.getAbsolutePath() + ".*?:([0-9]+):[0-9]+(,\\s.*)?>.*"; 
			String regexStartLine3 = "<.*>\\sline:([0-9]+)(:[0-9]+)";
			String regexEndLine = "line:([0-9]+)(:[0-9]+>)";
			Pattern pStartLine1= Pattern.compile(regexStartLine1);
			Pattern pStartLine2 = Pattern.compile(regexStartLine2);
			Pattern pStartLine3 = Pattern.compile(regexStartLine3);
			Pattern pEndLine = Pattern.compile(regexEndLine);
			Matcher mStartLine1, mStartLine2, mStartLine3, mEndLine;
			
			String regexVarDecl = "(Parm)?VarDecl\\s(0x[0-9a-z]+)\\s";
//			String regexVarUse = "\\s(Parm)?Var\\s(0x[0-9a-z]+)\\s'([_a-zA-Z]+[_a-zA-Z0-9]*)'\\s'([_a-zA-Z]+[_a-zA-Z0-9\\s\\(\\)\\*\\[\\]]*)'";	// Var id '变量名' '类型'
			String regexVarUse = "\\s(Parm)?Var\\s(0x[0-9a-z]+)\\s'([_a-zA-Z]+[_a-zA-Z0-9]*)'";
			Pattern pVarDecl = Pattern.compile(regexVarDecl);
			Pattern pVarUse = Pattern.compile(regexVarUse);
//			Pattern pVaList = Pattern.compile(regexVaList);
			Matcher mVarDecl, mVarUse;
//			Matcher mVaList;
			
			String regexEqualOp1 = ".*AssignOperator\\s(0x[a-z0-9]+)\\s<.*>\\s'([_a-zA-Z]+[_a-zA-Z0-9\\s\\(\\)\\*\\[\\]]*)'\\s'.=";
			String regexEqualOp2 = ".*BinaryOperator\\s(0x[a-z0-9]+)\\s<.*>\\s'([_a-zA-Z]+[_a-zA-Z0-9\\s\\(\\)\\*\\[\\]]*)'\\s'=";
			Pattern pEqualOp1 = Pattern.compile(regexEqualOp1);
			Pattern pEqualOp2 = Pattern.compile(regexEqualOp2);
			Matcher mEqualOp1, mEqualOp2;
			
			Stack<EqualOperatorBlock> equalOpStack = new Stack<EqualOperatorBlock>();
			Stack<VarDeclareBlock> varDeclStack = new Stack<VarDeclareBlock>();
			EqualOperatorBlock topEqualOpBlock, popEqualOpBlock;
			VarDeclareBlock topVarDeclBlock, popVarDeclBlock;
			
			String regexStructUnionDecl = "\\bRecordDecl\\s(0x[a-z0-9]+)(\\s[a-z]+\\s0x[0-9a-z]+)?\\s<.*>.*\\s(\\bstruct\\b)?(\\bunion\\b)?\\s([_a-zA-Z]+[_a-zA-Z0-9]*)"; //definition
			String regexFieldDecl = "\\bFieldDecl\\s(0x[a-z0-9]+)\\s<.*>.*?(\\breferenced\\b)?\\s([_a-zA-Z]+[_a-zA-Z0-9]*)\\s'([_a-zA-Z]+[_a-zA-Z0-9,\\.\\s\\(\\)\\*\\[\\]]*)'"; 
			Pattern pStructUnionDecl = Pattern.compile(regexStructUnionDecl);
			Pattern pFieldDecl = Pattern.compile(regexFieldDecl);
			Matcher mStructUnionDecl, mFieldDecl;
			Stack<StructUnionBlock> suBlockStack = new Stack<StructUnionBlock>();
			StructUnionBlock topSUBlock, popSUBlock;
			
			int startLine = 0, endLine = 0, tempLine = 0;
			int astTempLine = 0;
			int topEndLine = 0;
			AstVariable newVar = null;
			AstVariable useVar = null;
			EqualOperatorBlock equalOpBlock;
			VarDeclareBlock varDeclBlock;
			
			//一、遍历ast所有行
			for(String line: astList) {
				astTempLine++;
				//1.update tempLine
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
				startLine = tempLine;
				mEndLine = pEndLine.matcher(line);
				if(mEndLine.find()) {
					endLine = Integer.parseInt(mEndLine.group(1));
				}else {
					endLine = tempLine;
				}
				
				//if(tempLine <= 26) System.out.println("line" + tempLine + ":\n" + line +"\n");
				//2.1 pop出VarDecl
				//用来临时存放pop出的vardecl，为了使同行的vardecl按照声明的先后顺序放入vardeclblockList
				//如 int a = 1, c = a; 先pop出c = a，再pop出a = 1，则按照原顺序放入tempList
				List<VarDeclareBlock> tempVarDeclList = new ArrayList<VarDeclareBlock>();
				int declAstEndLine = astTempLine-1;
				while(!varDeclStack.isEmpty()) {
					topVarDeclBlock = varDeclStack.peek();
					topEndLine = topVarDeclBlock.getEndLine();
					if(tempLine <= topEndLine) {	
						break;
					}else {
						popVarDeclBlock = varDeclStack.pop();
						popVarDeclBlock.setAstEndLine(declAstEndLine);	
						tempVarDeclList.add(popVarDeclBlock);
						if(!varDeclStack.isEmpty()) {
							//并列声明的情况，先声明的astEndLine是后声明的astStartLine-1
							declAstEndLine = varDeclStack.peek().getAstStartLine()-1;
						}
					}
				}
				if(!tempVarDeclList.isEmpty()) varDeclBlocks.addAll(tempVarDeclList);
				
				//2.2 同理，pop出equalOpBlock
				List<EqualOperatorBlock> tempEuqalOpList = new ArrayList<EqualOperatorBlock>();
				int equalAstEndLine = astTempLine-1;
				while(!equalOpStack.isEmpty()) {
					topEqualOpBlock = equalOpStack.peek();
					topEndLine = topEqualOpBlock.getEndLine();
					if(tempLine <= topEndLine) {	
						break;
					}else {
						popEqualOpBlock = equalOpStack.pop();
						popEqualOpBlock.setAstEndLine(equalAstEndLine);	
						tempEuqalOpList.add(popEqualOpBlock);
						if(!equalOpStack.isEmpty()) {
							equalAstEndLine = equalOpStack.peek().getAstStartLine()-1;
						}
					}
				}
				if(!tempEuqalOpList.isEmpty()) equalOpBlocks.addAll(tempEuqalOpList);
				
				//2.3 同理 StructUnionBlock
				int suAstEndLine = astTempLine-1;
				while(!suBlockStack.isEmpty()) {
					topSUBlock = suBlockStack.peek();
					topEndLine = topSUBlock.getEndLine();
					if(tempLine <= topEndLine) {	
						break;
					}else {
						popSUBlock = suBlockStack.pop();
						popSUBlock.setAstEndLine(suAstEndLine);	
						if(!suBlockStack.isEmpty()) {
							suAstEndLine = suBlockStack.peek().getAstStartLine()-1;
						}
					}
				}
				
				//3.1 newVar and varDeclBlock
				mVarDecl = pVarDecl.matcher(line);
				if(mVarDecl.find()) {
					String varid = mVarDecl.group(2);
					String varname;
					String vartype;
					String varkind;
					boolean isParm = false;
					boolean isCinit = false;
					boolean isUsed = false;
					boolean isConst = false;
					int type_start_index = line.indexOf("'",line.indexOf(">")) + 1;	//type开始下标
					int type_end_index = line.indexOf("'", type_start_index);	//type结束后的'下标
					int name_end_index = type_start_index - 2;	//name后的空格下标
					int name_start_index = line.lastIndexOf(" ", name_end_index-1) + 1;	//name开始下标
					varname = line.substring(name_start_index, name_end_index);
					vartype = line.substring(type_start_index, type_end_index);
					if(vartype.contains("const")) {
						isConst = true;
					}
					vartype = typeSimplify(vartype);
					
					varkind = AstVariable.getVarKindByType(vartype);
					if(varkind.equals("pointer")) {
						newVar = new PointerVar(varid, varname, vartype, isConst);
					}else if(varkind.equals("array")) {
						//System.out.println("[array]:");
						//System.out.println("type: " + vartype);
						newVar = new ArrayVar(varid, varname, vartype, isConst);
					}else {
						newVar = new CommonVar(varid, varname, vartype, isConst);
					}
					newVar.setDeclareline(tempLine);
					newVar.setKind(varkind);
					
					if(mVarDecl.group(1) != null) {
						isParm = true;
						isCinit = true;
					}
					if(line.contains(" cinit")) {
						isCinit = true;
					}
					if(line.contains(" used ")){
						isUsed = true;
					}
					newVar.setIsParmVar(isParm);
					newVar.setIsInitialized(isCinit);
					newVar.setIsUsed(isUsed);
				
					//if(newVar.getKind().equals("array")) System.out.println(newVar.getDeclareLine() +"\n");
					allVarsMap.put(varid, newVar);	//(1)变量放入allVarsMap中
					//(2)将newvar放入fileList中的当前行
					if(lineDeclMap.containsKey(tempLine)) {
						lineDeclMap.get(tempLine).add(varid);
					}else {
						ArrayList<String> lineUseList = new ArrayList<String>();
						lineUseList.add(varid);
						lineDeclMap.put(tempLine, lineUseList);
					}
					
//					System.out.println(line);
//					System.out.println("newVar: " + newVar.getName() + " " + newVar.getId() + " " + newVar.getDeclareLine() + " " + newVar.getIsIntialized());
					
					//(3) 创建varDeclBlock，加入时astTempLine是block的astStartLine,astEndLine在pop的时候才知道
					varDeclBlock = new VarDeclareBlock(varid, startLine, endLine, astTempLine, 0);
					if(isCinit == true) {
						varDeclBlock.setIsCinit(true);
						//同理，创建equalOpBlock，加入时astTempLine是block的astStartLine,astEndLine在pop的时候才知道
						String equalId = varid, equalCalcuType = vartype;
						equalOpBlock = new EqualOperatorBlock(equalId, equalCalcuType, startLine, endLine, astTempLine, 0);
						equalOpBlock.setIsCinit(true);
						equalOpStack.push(equalOpBlock);
						/*tip: newAdd*/
						if(equalOpMap.containsKey(tempLine)) {
							equalOpMap.get(tempLine).add(equalOpBlock);
						}else {
							ArrayList<EqualOperatorBlock> lineEqualOpList = new ArrayList<EqualOperatorBlock>();
							lineEqualOpList.add(equalOpBlock);
							equalOpMap.put(tempLine, lineEqualOpList);
						}
					}
					varDeclStack.push(varDeclBlock);
					varDeclBlockMap.put(varid, varDeclBlock);
				}
				
				//3.2. new Equal
				mEqualOp1 = pEqualOp1.matcher(line);
				mEqualOp2 = pEqualOp2.matcher(line);
				if(mEqualOp1.find()) {
					//System.out.println(tempLine);
					String equalId = mEqualOp1.group(1);
					String equalCalcuType = mEqualOp1.group(2);
					equalOpBlock = new EqualOperatorBlock(equalId, equalCalcuType, startLine, endLine, astTempLine, 0);
					equalOpStack.push(equalOpBlock);
					/*tip: newAdd*/
					if(equalOpMap.containsKey(tempLine)) {
						equalOpMap.get(tempLine).add(equalOpBlock);
					}else {
						ArrayList<EqualOperatorBlock> lineEqualOpList = new ArrayList<EqualOperatorBlock>();
						lineEqualOpList.add(equalOpBlock);
						equalOpMap.put(tempLine, lineEqualOpList);
					}
				}else if(mEqualOp2.find()) {
					String equalId = mEqualOp2.group(1);
					String equalCalcuType = mEqualOp2.group(2);
					equalOpBlock = new EqualOperatorBlock(equalId, equalCalcuType, startLine, endLine, astTempLine, 0);
					equalOpStack.push(equalOpBlock);
					/*tip: newAdd*/
					if(equalOpMap.containsKey(tempLine)) {
						equalOpMap.get(tempLine).add(equalOpBlock);
					}else {
						ArrayList<EqualOperatorBlock> lineEqualOpList = new ArrayList<EqualOperatorBlock>();
						lineEqualOpList.add(equalOpBlock);
						equalOpMap.put(tempLine, lineEqualOpList);
					}
				}
				
				//4.1 new StructUnionBlock
				mStructUnionDecl = pStructUnionDecl.matcher(line);
				if(mStructUnionDecl.find()) {
//					System.out.println(line);
					String id = mStructUnionDecl.group(1);
					String block_type = mStructUnionDecl.group(3);
					if(block_type == null) block_type = mStructUnionDecl.group(4);
					String name = mStructUnionDecl.group(5);
					StructUnionBlock newSUBlock = new StructUnionBlock(id, name, block_type);
					newSUBlock.setStartLine(startLine);
					newSUBlock.setEndLine(endLine);
					newSUBlock.setAstStartLine(astTempLine);
					if(!suBlockStack.isEmpty()) {
						topSUBlock = suBlockStack.peek();
						topSUBlock.getChildStructUnion().add(newSUBlock);
						newSUBlock.setParentStructUnion(topSUBlock);
					}
					suBlockStack.push(newSUBlock);
					allStructUnionMap.put(id, newSUBlock);
				}
				
				///4.2 new FieldVar
				mFieldDecl = pFieldDecl.matcher(line);
				if(mFieldDecl.find() && !suBlockStack.isEmpty()) {
//					System.out.println(line);
					String id = mFieldDecl.group(1);
					String name = mFieldDecl.group(3);
					String type = mFieldDecl.group(4);
					boolean isConst = false;
					if(type.contains("const")) {
						isConst = true;
					}
					type = typeSimplify(type);
					FieldVar newFieldVar = new FieldVar(id, name, type, isConst);
					newFieldVar.setDeclareline(startLine);
					if(mFieldDecl.group(2) != null) {
						newFieldVar.setIsReferenced(true);
					}else {
						newFieldVar.setIsReferenced(false);
					}
					topSUBlock = suBlockStack.peek();
					topSUBlock.getChildField().add(newFieldVar);
					newFieldVar.setParentStructUnion(topSUBlock);
					allFieldVarsMap.put(id, newFieldVar);
				}
				
				//5. varUse
				mVarUse = pVarUse.matcher(line);
				if(mVarUse.find()) {
					String useVarid = mVarUse.group(2);
					useVar = allVarsMap.get(useVarid);
					useVar.useLine.add(tempLine);
					/*tip: */
					useVar.setIsUsed(true);
					if(lineUseMap.containsKey(tempLine)) {
						lineUseMap.get(tempLine).add(useVarid);
					}else {
						ArrayList<String> lineUseVars = new ArrayList<String>();
						lineUseVars.add(useVarid);
						lineUseMap.put(tempLine, lineUseVars);
					}
				}
				
			}
			
			//二，遍历完ast
			//1.在最后一行有声明的情况e.g { ....// lastFileLine:  int a, b; return 0;}
			//按声明顺序放入declBlocks
			for( int i = 0; i < varDeclStack.size(); i++) {
				int declAstEndLine;
				VarDeclareBlock tempVarDeclBlock = varDeclStack.get(i);
				if(i < varDeclStack.size()-1) {
					declAstEndLine = varDeclStack.get(i+1).getAstStartLine()-1;
				}else declAstEndLine = astTempLine;
				tempVarDeclBlock.setAstEndLine(declAstEndLine);	
				varDeclBlocks.add(tempVarDeclBlock);
			}
			varDeclStack.clear();
			
			genVarDeclBlockInform();
			
			//2.同理，考虑在最后一行有=的情况
			//按=顺序放入equalOpBlocks
			for( int i = 0; i < equalOpStack.size(); i++) {
				int equalAstEndLine;
				EqualOperatorBlock tempEqualOpBlock = equalOpStack.get(i);
				if( i < equalOpStack.size()-1) {
					equalAstEndLine = equalOpStack.get(i+1).getAstStartLine()-1;
				}else equalAstEndLine = astTempLine;
				tempEqualOpBlock.setAstEndLine(equalAstEndLine);	//当前astTempLine就是ast最后一行
				equalOpBlocks.add(tempEqualOpBlock);
			}
			equalOpStack.clear();
			
			genEblockInform();
			
			setVariableGlobalAttribute();
			
			//System.out.println("last:" + astList.get(astList.size()-1));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private String typeSimplify(String type) {
		//e.g. const uint64_t ***[3]; struct S4 *const *
		type = type.replace("const", "").trim();
		type = type.replace("volatile", "").trim();
		type = type.replaceAll("\\s\\s+", " ").trim();	//两个类型之间只有一个空格
		type = type.replaceAll("\\*\\s+", "*").trim();	//指针与右边没有空格 
		type = type.replace(" [", "[").trim();	//数组与左边没有空格
		return type;
	}
	
//	private List<AstVariable> getAllGlobalVars(List<AstVariable> allVars){
//		List<AstVariable> allGlobalVars = new ArrayList<AstVariable>();
//		for(AstVariable var: allVars) {
//			if(var.getIsGlobal()) {
//				allGlobalVars.add(var);
//			}
//		}
//		return allGlobalVars;
//	}
	
	
	public List<AstVariable> getAllGlobalVars(){
		List<AstVariable> allGlobalVars = new ArrayList<AstVariable>();
		List<AstVariable> allVars = getAllVars();
		for(AstVariable var: allVars) {
			if(var.getIsGlobal()) {
				allGlobalVars.add(var);
			}
		}
		return allGlobalVars;
	}
	
	public List<AstVariable> getAllVars(){
		return allVarsMap.values().stream().collect(Collectors.toList());
	}
	
	private void setVariableGlobalAttribute() {
		List<FunctionBlock> allFunctions = getAllFunctionBlocks();
		for(AstVariable var:allVarsMap.values()) {
			//参数非globalVars
			if(var.getIsParmVar() == true) continue;
			//是否在函数内声明
			int declareline = var.getDeclareLine();
//			System.out.println("varname: " + var.getName());
//			System.out.println("declareline: " + declareline);
			boolean isGlobalVar = true;
			for(FunctionBlock fun: allFunctions) {
//				System.out.println("fun: " +fun.name + " " + fun.startline +" " + fun.endline);
				if(fun.startline <= declareline && declareline <= fun.endline) {
					isGlobalVar = false;
//					System.out.println("varname:" + var.getName());
					break ;
				}
			}
//			System.out.println();
			if(isGlobalVar == true) {
				var.setIsGlobal(true);
			}
		}
	}
	
	
	public Set<String> getGlobalVarsSet(Map<String, AstVariable> allVarsMap, List<FunctionBlock> allFunctions){
		Set<String> globalVarsSet = new HashSet<String>();
		for(AstVariable var:allVarsMap.values()) {
			//参数非globalVars
			if(var.getIsParmVar() == true) continue;
			//是否在函数内声明
			int declareline = var.getDeclareLine();
//			System.out.println("varname: " + var.getName());
//			System.out.println("declareline: " + declareline);
			boolean isGlobalVar = true;
			for(FunctionBlock fun: allFunctions) {
//				System.out.println("fun: " +fun.name + " " + fun.startline +" " + fun.endline);
				if(fun.startline <= declareline && declareline <= fun.endline) {
					isGlobalVar = false;
//					System.out.println("varname:" + var.getName());
					break ;
				}
			}
//			System.out.println();
			if(isGlobalVar == true) {
				globalVarsSet.add(var.getId());
				var.setIsGlobal(true);
			}
		}
		
		return globalVarsSet;
	}
	
	public List<FunctionBlock> getAllFunctionBlocks(){
		List<FunctionBlock> allBlocks = new ArrayList<FunctionBlock>();
		FunctionBlock block = null;
		String regexFunDecl1 = ".*\\bFunctionDecl\\s(0x[a-z0-9]+)\\s<.*>.*\\s([0-9a-zA-Z_]+)\\s'.*'(\\sextern){0,1}";
		String regexFunDecl2 = ".*\\bFunctionDecl\\s(0x[a-z0-9]+)\\sprev\\s0x[a-z0-9]+\\s<.*>.*\\s([0-9a-zA-Z_]+)\\s'.*'(\\sextern){0,1}";
		String regexCompStmt = ".*\\bCompoundStmt\\s.*";
		String regexReturnStmt = ".*\\bReturnStmt\\s.*";
		String regexStartLine1 = "<.*>\\sline:([0-9]+)(:[0-9]+)";
		//String regexStartLine2 = ".*[a-zA-Z]+\\s0x[a-z0-9]+\\s<" + filepath + ".*:([0-9]+):[0-9]+(,\\s.*)?>.*"; 
		String regexStartLine2 = ".*[a-zA-Z]+\\s0x[a-z0-9]+\\s<" + file.getAbsolutePath() + ".*?:([0-9]+):[0-9]+(,\\s.*)?>.*"; 
		String regexStartLine3 = "<line:([0-9]+)(:[0-9]+)";
		String regexEndLine = "line:([0-9]+)(:[0-9]+>)";
		Pattern pFunDecl1 = Pattern.compile(regexFunDecl1);
		Pattern pFunDecl2 = Pattern.compile(regexFunDecl2);
		Pattern pCompStmt = Pattern.compile(regexCompStmt);
		Pattern pReturnStmt = Pattern.compile(regexReturnStmt);
		Pattern pStartLine1= Pattern.compile(regexStartLine1);
		Pattern pStartLine2 = Pattern.compile(regexStartLine2);
		Pattern pStartLine3 = Pattern.compile(regexStartLine3);
		Pattern pEndLine = Pattern.compile(regexEndLine);
		Matcher mFunDecl1, mFunDecl2, mCompStmt, mReturnStmt;
		Matcher mStartLine1, mStartLine2, mStartLine3, mEndLine;
		int startline = 0, endline = 0, templine = 0;
		int functionEndLine = 0;	//记录当前function的ast是否遍历完
		boolean findCompStmt = false;
		for(String line: astList) {
			mStartLine1 = pStartLine1.matcher(line);
			mStartLine2 = pStartLine2.matcher(line);
			mStartLine3 = pStartLine3.matcher(line);
			if(mStartLine1.find()) {
				templine = Integer.parseInt(mStartLine1.group(1));
			}else if(mStartLine2.find()) {
				templine = Integer.parseInt(mStartLine2.group(1));
			}else if(mStartLine3.find()) {
				templine = Integer.parseInt(mStartLine3.group(1));
			}
			startline = templine;
			mEndLine = pEndLine.matcher(line);
			if(mEndLine.find()) {
				endline = Integer.parseInt(mEndLine.group(1));
			}else {
				endline = templine;
			}
			
			mFunDecl1 = pFunDecl1.matcher(line);
			mFunDecl2 = pFunDecl2.matcher(line);
			if(mFunDecl1.find()) {
				block = new FunctionBlock();
				block.id = mFunDecl1.group(1);
				block.name = mFunDecl1.group(2);
				block.startline = startline;
				block.endline = endline;
				if(mFunDecl1.group(3) != null) block.ifExtern = true;
				else block.ifExtern = false;
				findCompStmt = true;
				functionEndLine = endline;
				continue ;
			}else if(mFunDecl2.find()) {
				block = new FunctionBlock();
				block.id = mFunDecl2.group(1);
				block.name = mFunDecl2.group(2);
				block.startline = startline;
				block.endline = endline;
				if(mFunDecl2.group(3) != null) block.ifExtern = true;
				else block.ifExtern = false;
				findCompStmt = true;
				functionEndLine = endline;
				continue ;
			}
			if(functionEndLine < templine) {
				block = null;
				continue ;
			}
			
			mReturnStmt = pReturnStmt.matcher(line);
			if(mReturnStmt.find()) {
				if(block!=null) {
					if(block.returnLineSet == null) {
						block.returnLineSet = new TreeSet<Integer>();
					}
					block.returnLineSet.add(templine);
				}
			}
						
			if(findCompStmt){
				mCompStmt = pCompStmt.matcher(line);
				if(mCompStmt.find()) {
					block.leftBraceLine = startline;
					block.rightBraceLine = endline;
					allBlocks.add(block);
					findCompStmt = false;	//functionCompStmt已找到
				}else continue;
			}
			
		}
		
		return allBlocks;
	}
	
	private void genVarDeclBlockInform() {
		for(VarDeclareBlock block: this.varDeclBlocks) {
			//若是声明时初始化，eblockid就是leftVarid
			String regexVarUse = "\\s(Parm)?Var\\s(0x[0-9a-z]+)\\s'([_a-zA-Z]+[_a-zA-Z0-9]*)'\\s'([_a-zA-Z]+[_a-zA-Z0-9\\s\\(\\)\\*\\[\\]]*)'";	
			Pattern p = Pattern.compile(regexVarUse);
			Matcher m;
			
			block.setLeftVar(block.getId());
			if(block.getIsCinit() == true) {
				for(int temp = block.getAstStartLine(); temp < block.getAstEndLine(); temp++) {
					String line = astList.get(temp);
					m = p.matcher(line);
					if(m.find()) {
						block.getRightVar().add(m.group(2));
					}
				}
			}
			if(block.getEndLine() < block.getStartLine()) {
				
			}
		}
	}
	
	private void genEblockInform() {
		String regexVarUse = "\\s(Parm)?Var\\s(0x[0-9a-z]+)\\s'([_a-zA-Z]+[_a-zA-Z0-9]*)'\\s'([_a-zA-Z]+[_a-zA-Z0-9\\s\\(\\)\\*\\[\\]]*)'";	
		Pattern p = Pattern.compile(regexVarUse);
		Matcher m;
		//等式EqualBlock右边是否使用函数？
		String regexEuqalOpFunc = "<.*>.*\\sFunction\\s0x[a-z0-9]+\\s'([_a-zA-Z]+[_a-zA-Z0-9]*)'\\s'.*'";
		Pattern pEqualOpFunc = Pattern.compile(regexEuqalOpFunc);
		Matcher mEqualOpFunc;
		
		for(EqualOperatorBlock eblock: this.equalOpBlocks) {
			int begin = eblock.getAstStartLine();
			int end = eblock.getAstEndLine();
			int temp;
			
			//若是声明时初始化，eblockid就是leftVarid
			if(eblock.getIsCinit() == true) {
				eblock.setLeftVar(eblock.getId());
				for(temp = begin; temp < end; temp++) {
					String line = astList.get(temp);
					m = p.matcher(line);
					if(m.find()) {
						eblock.getRightVar().add(m.group(2));
						if(eblock.getRightFirstVarAstLine() == 0) eblock.setRightFirstVarAstLine(temp+1);
					}
					mEqualOpFunc = pEqualOpFunc.matcher(line);
					if(mEqualOpFunc.find()) {
						eblock.setHasFunction(true);
						if(eblock.getRightFirstFunAstLine() == 0 ) eblock.setRightFirstFunAstLine(temp+1);
						eblock.getFunctionName().add(mEqualOpFunc.group(1));
					}
				}
				
			}else {
				//find leftVar
				for(temp = begin-1 ; temp < end; temp++) {
					String line = astList.get(temp);
					m = p.matcher(line);
					if(m.find()) {
						eblock.setLeftVar(m.group(2));
						break;
					}
				}
				for(temp = temp+1; temp < end; temp++) {
					String line = astList.get(temp);
					m = p.matcher(line);
					if(m.find()) {
						eblock.getRightVar().add(m.group(2));
						if(eblock.getRightFirstVarAstLine() == 0) eblock.setRightFirstVarAstLine(temp+1);
					}
					mEqualOpFunc = pEqualOpFunc.matcher(line);
					if(mEqualOpFunc.find()) {
						eblock.setHasFunction(true);
						if(eblock.getRightFirstFunAstLine() == 0 ) eblock.setRightFirstFunAstLine(temp+1);
						eblock.getFunctionName().add(mEqualOpFunc.group(1));
					}
				}
			}
		}
	}
	
	public static List<String> testProcessThread(String command) throws IOException, InterruptedException{
		
		String[] cmd = new String[] { "/bin/bash", "-c", command };
		ProcessBuilder builder = new ProcessBuilder(cmd);
		builder.redirectErrorStream(true);
		Process proc = builder.start();
		
		ProcessWorker pw = new ProcessWorker(proc);
		pw.start();
		ProcessStatus ps = pw.getPs();
		try {
			pw.join(5 * 1000);
			if(ps.exitCode == ps.CODE_STARTED) {
				pw.interrupt();
				List<String> result = new ArrayList<String>();
				result.add("timeout");
				return result;
			}
			else {
				return ps.output;
			}
		}catch(InterruptedException e) {
			pw.interrupt();
			throw e;
		}finally {
			proc.destroy();
		}
	}	
	
}

