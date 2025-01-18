package astinfo;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import astinfo.model.*;
import utity.AvailableVariable;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.io.File;

import astinfo.Inform_Gen.*;

public class VarInform {

	public static void main(String[] args) {
		//TODO Auto-generated method stub
		
		File file = new File("/home/jing/Desktop/random3.c");
		AstInform_Gen ast = new AstInform_Gen(file);
		List<AstVariable> allGlobalVars = ast.getAllGlobalVars();
		LoopInform_Gen loops = new LoopInform_Gen(ast);
		System.out.println("outloopcnt = " + loops.outmostLoopList.size());

		for(LoopStatement loop: loops.outmostLoopList) {
			List<AstVariable> invars = getBlockGlobalInvars(allGlobalVars, loop.getUseVarList());
			List<AvailableVariable> avarList = getInitialAvailableVarList(invars, ast);
			for(AvailableVariable avar: avarList) {
				System.out.println(avar.getType() + " " + avar.getValue() + " " + avar.getIsConst() );
			}
			System.out.println();
		}
		
	}
	
	public static List<AstVariable> getBlockGlobalInvars(List<AstVariable> allGlobalVars, List<AstVariable> useVarList){
		List<AstVariable> res = new ArrayList<AstVariable>();
		for(AstVariable global: allGlobalVars) {
			boolean flag = false;
			for(AstVariable use: useVarList) {
				if(global == use) {
					flag = true;
					break ;
				}
			}
			if(flag == false) {
				res.add(global);
			}
		}
		
		return  res;
	}
	
	public static List<AvailableVariable> getInitialAvailableVarList(List<AstVariable> varList, AstInform_Gen ast){
		List<AvailableVariable> res = new ArrayList<AvailableVariable>();
		List<StructUnionBlock> suBlocks = ast.allStructUnionMap.values().stream().collect(Collectors.toList());
		for(AstVariable var: varList) {
			String varname = var.getName();
			String varkind = var.getKind();
			String vartype = var.getType();
			String value, type;
			boolean varIsConst, isConst;
			varIsConst = isConst = var.getIsConst();
			boolean isStructUnion = var.getIsStructUnion();
			
			if(varkind.equals("common")) {
				if(isStructUnion) {
					StructUnionBlock su = searchStructUnion(vartype, suBlocks);
					for(FieldVar field: su.getChildField()) {
						if(field.getIsStructUnion()) continue ;
						value = varname + "." + field.getName();
						type = field.getType();
						if(varIsConst == false) {
							isConst = field.getIsConst();
						}else isConst = true;
						res.add(new AvailableVariable(value, type, isConst));
					}
				}else {
					res.add(new AvailableVariable(varname, var.getType(), isConst));
				}
			}else if(varkind.equals("pointer")) {
				String star = PointerVar.getLevelStar(vartype);
				if(isStructUnion) {
					//e.g. struct S *p = &s;
					//(*p).field
					StructUnionBlock su = searchStructUnion(vartype, suBlocks);
					for(FieldVar field: su.getChildField()) {
						if(field.getIsStructUnion()) continue ;
						value = "(" + star + varname + ")." + field.getName();
						type = field.getType();
						if(varIsConst == false) {
							isConst = field.getIsConst();
						}else isConst = true;
						res.add(new AvailableVariable(value, type, isConst));
					}
				}else {
					value = "(" + star + varname + ")";
					type = vartype.replaceAll("\\*", "").trim();
					res.add(new AvailableVariable(value, type, isConst));
				}
				
			}else if(varkind.equals("array")){
				String eletype = ArrayVar.getEleTypeByType(vartype);
				String elekind = ArrayVar.getEleKindByType(vartype);
				List<String> array_index_list = getArrayIndexList(vartype);		//得到所有的元素下标
				
				if(elekind.equals("common")) {
					if(isStructUnion) {
						StructUnionBlock su = searchStructUnion(vartype, suBlocks);
						for(String index: array_index_list) {
							for(FieldVar field: su.getChildField()) {
								if(field.getIsStructUnion()) continue ;
								value = varname + index + "." + field.getName();
								type = field.getType();
								if(varIsConst == false) {
									isConst = field.getIsConst();
								}else isConst = true;
								res.add(new AvailableVariable(value, type, isConst));
							}
						}
					}else {
						for(String index: array_index_list) {
								value = varname + index;
								type = eletype;
								res.add(new AvailableVariable(value, type, isConst));
						}
					}
				}else if(elekind.equals("pointer")) {
					String star = PointerVar.getLevelStar(eletype);
					if(isStructUnion) {
						//e.g. struct S *p[2];
						//(*p[0]).field
						StructUnionBlock su = searchStructUnion(vartype, suBlocks);
						for(String index: array_index_list) {
							for(FieldVar field: su.getChildField()) {
								if(field.getIsStructUnion()) continue ;
								value = "(" + star + varname + index + ")." + field.getName();
								type = field.getType();
								if(varIsConst == false) {
									isConst = field.getIsConst();
								}else isConst = true;
								res.add(new AvailableVariable(value, type, isConst));
								isConst = false;
							}
						}
					}else {
						//e.g. int *p[2];
						//(*p[0])
						for(String index: array_index_list) {
							value = "(" + star + varname + index + ")";
							type = eletype.replaceAll("\\*", "").trim();
							res.add(new AvailableVariable(value, type, isConst));
						}
					}
				}
			}
		}
		
		return res;
		
	}
	
	public static List<String> getArrayIndexList(String vartype){
		List<String> res = new ArrayList<String>();
		
		//最多三层循环
		int dim1 = -1, dim2 = -1, dim3 = -1;
		String regexDim = "\\[([0-9]+)";
		Pattern pDim = Pattern.compile(regexDim);
		Matcher mDim = pDim.matcher(vartype);
		while(mDim.find()) {
			if(dim1 == -1) {
				dim1 = Integer.valueOf(mDim.group(1));
			}else if(dim2 == -1) {
				dim2 = Integer.valueOf(mDim.group(1));
			}else {
				dim3 = Integer.valueOf(mDim.group(1));
			}
		}
		String dim1_index = "", dim2_index = "", dim3_index = "";
		for(int dim1_t = 0; dim1_t < dim1; dim1_t++) {
			dim1_index = "[" + dim1_t + "]";
			for( int dim2_t = 0; dim2_t < dim2; dim2_t++) {
				dim2_index = "[" + dim2_t + "]";
				for(int dim3_t = 0; dim3_t < dim3; dim3_t++) {
					dim3_index = "[" + dim3_t + "]";
					res.add(dim1_index + dim2_index + dim3_index);	//三维数组
				}
				if(dim3 == -1) {
					res.add(dim1_index + dim2_index);	//二维数组
				}
			}
			if(dim2 == -1) {
				res.add(dim1_index);		//一维数组
			}
		}
		return res;
	}
	
	public static StructUnionBlock searchStructUnion(String vartype, List<StructUnionBlock> suBlocks) {
		StructUnionBlock res = null;
		String su_name = null;
		String regexS = "struct (S[0-9]+)";
		String regexU = "union (U[0-9]+)";
		Pattern pS = Pattern.compile(regexS);
		Pattern pU = Pattern.compile(regexU);
		Matcher mS = pS.matcher(vartype);
		Matcher mU = pU.matcher(vartype);
		if(mS.find()) {
			su_name = mS.group(1);
		}else if(mU.find()) {
			su_name = mU.group(1);
		}
		
		for(StructUnionBlock su: suBlocks) {
			if(su.getName().equals(su_name)) {
				res = su;
				break;
			}
		}
		
		return res;
	}

	
	public static List<AstVariable> getBlockInvars(List<FunctionBlock> allFunctions, int block_startline, List<AstVariable> block_usevarlist, Map<String, AstVariable> allVarsMap) {
		
		List<AstVariable> res = new ArrayList<AstVariable>();
		FunctionBlock func = getInWhichFunc(allFunctions, block_startline);
		if(func == null) return res;
		
		Set<String> usevar_id = new HashSet<String>();
		for(AstVariable usevar: block_usevarlist) {
			usevar_id.add(usevar.getId());
		}
		for(AstVariable var: allVarsMap.values()) {
			if(usevar_id.contains(var.getId())) {
				continue;
			} else{
				int declareline = var.getDeclareLine();
				if(var.getIsGlobal() && declareline <= func.startline) {
					res.add(var);
				} else if(declareline >= func.startline && declareline < block_startline) {
					res.add(var);
				}
			}
		}
		
		return res;
		
	}
	
	public static FunctionBlock getInWhichFunc(List<FunctionBlock> allFunctions, int line) {
			FunctionBlock func = null;
			
			for(FunctionBlock f: allFunctions) {
				if(f.startline <= line && f.endline >=line) {
					func = f;
					break ;
				}
			}
			return func;
	}
}
