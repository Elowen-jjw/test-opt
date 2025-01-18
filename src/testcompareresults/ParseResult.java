package testcompareresults;

import objectoperation.list.CommonOperation;
import utity.CompilationInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;

public class ParseResult {
	public Map<String, Boolean> parseSanitizerInfoList(List<CompilationInfo> CompilationInfoList) {
		Map<String, Boolean> resMap = new HashMap<String, Boolean>();
		
		resMap.put("sanitizer_error", false);
		resMap.put("sanitizer_timeout", false);
		resMap.put("sanitizer_incon", false);
		resMap.put("sanitizer_pass", false);

		if(CompilationInfoList.isEmpty()) {
			return resMap;
		}
		
		if(!parseSanitizerPass(CompilationInfoList)){
			return resMap;
		} else {
			resMap.put("sanitizer_pass", true);

			if(searchSanitizerResultLabel(CompilationInfoList, "error")) {
				resMap.put("sanitizer_error", true);
			}
			if(searchSanitizerResultLabel(CompilationInfoList, "timeout")) {
				resMap.put("sanitizer_timeout", true);
			}
			if(searchSanitizerResultLabel(CompilationInfoList, "sanitize")) {
				resMap.put("sanitizer_incon", true);
			}
		}

		return resMap;
	}
	
	public Map<String, Boolean> parseOutputInfoList(List<CompilationInfo> CompilationInfoList){
		Map<String, Boolean> resMap = new HashMap<String, Boolean>();
		resMap.put("output_error", false);
		resMap.put("output_timeout", false);
		resMap.put("output_inconsistent", false);

		if(CompilationInfoList.isEmpty()) return resMap;
		
		if(parseOutputPass(CompilationInfoList)){
			return resMap;
		}else {
			if (searchOutputResultLabel(CompilationInfoList, "error")) {
				resMap.put("output_error", true);
			}
			if (searchOutputResultLabel(CompilationInfoList, "timeout")) {
				resMap.put("output_timeout", true);
			}
			if(searchOutputResultLabel(CompilationInfoList, "inconsistent")){
				resMap.put("output_inconsistent", true);
			}
		}
		return resMap;
	}

	public boolean parseOutputPass(List<CompilationInfo> CompilationInfoList){
		if(CompilationInfoList.isEmpty()) return true;

		List<String> optList = CompilationInfoList.get(0).getOutputListMap().keySet().stream().toList();
		List<String> standardOutput = CompilationInfoList.get(0).getOutputListMap().get(optList.get(0));
		String standardFirstLine = standardOutput.get(0);
		if(standardFirstLine.equals("timeout") || standardFirstLine.equals("error")) return false;

		for(CompilationInfo info: CompilationInfoList){
			for(List<String> compareOutput: info.getOutputListMap().values()){
				String compareFirstLine = compareOutput.get(0);
				if(compareFirstLine.equals("timeout") || compareFirstLine.equals("error")) return false;
				if(standardOutput.size() != compareOutput.size()) return false;
				for(int j = 0; j < standardOutput.size(); j++){
					if(!standardOutput.get(j).equals(compareOutput.get(j))) return false;
				}
			}
		}
		return true;
	}
	public boolean parseSanitizerPass(List<CompilationInfo> CompilationInfoList){
		if(CompilationInfoList.isEmpty()) return false;
		List<String> optList = CompilationInfoList.get(0).getOutputListMap().keySet().stream().toList();
		for(String opt: optList){
			List<String> initOutput = CompilationInfoList.get(0).getOutputListMap().get(opt);
			String initFirstLine = initOutput.get(0);
			if(initFirstLine.equals("timeout") || initFirstLine.equals("error") || initFirstLine.equals("sanitize")) continue;
			List<String> transOutput;
			int i;
			for(i = 1; i < CompilationInfoList.size(); i++){
				transOutput =  CompilationInfoList.get(i).getOutputListMap().get(opt);
				String transFirstLine = transOutput.get(0);
				if(transFirstLine.equals("timeout") || transFirstLine.equals("error") || transFirstLine.equals("sanitize")) break;
				if(initOutput.size() != transOutput.size()) break;
				int j;
				for(j = 0; j < initOutput.size(); j++){
					if(!initOutput.get(j).equals(transOutput.get(j))) break;
				}
				if(j != initOutput.size()) break;
			}
			if(i == CompilationInfoList.size()) return true;
		}
		return false;
	}

	public boolean searchSanitizerResultLabel(List<CompilationInfo> CompilationInfoList, String resultLabel){
		if(CompilationInfoList.isEmpty()) return false;
		for(CompilationInfo info: CompilationInfoList) {
			for(List<String> outputList: info.getOutputListMap().values()){
				String firstLine = outputList.get(0);
				if(firstLine.equals(resultLabel)) return true;
			}
		}
		return false;
	}

	public boolean searchOutputResultLabel(List<CompilationInfo> CompilationInfoList, String resultLabel){
		if(CompilationInfoList.isEmpty()) return false;
		Set<String> resultSet = new HashSet<String>();
		for(CompilationInfo info: CompilationInfoList) {
			for(List<String> outputList: info.getOutputListMap().values()){
				System.out.println();
				CommonOperation.printList(outputList);
				System.out.println();
				String firstLine = outputList.get(0);
				if(firstLine.equals("timeout") || firstLine.equals("error")) resultSet.add(outputList.get(0));
				else resultSet.add("inconsistent");
				if(resultLabel.equals("inconsistent")){
					if(resultSet.contains(resultLabel)) return true;    //inconsistent
				}else {
					if (resultSet.size() > 1 && resultSet.contains(resultLabel)) return true;    //timeout & error
				}
			}
		}
		return false;
	}

//	public boolean parseChecksum(List<CompilationInfo> CompilationInfoList) {
//		if(CompilationInfoList.isEmpty()) return false;
//
//		List<String> optList = CompilationInfoList.get(0).getOutputChecksumMap().keySet().stream().toList();
//		for(String opt: optList) {
//			Set<String> columnSet = new HashSet<String>();
//			for (CompilationInfo info : CompilationInfoList) {
//				columnSet.add(info.getOutputChecksumMap().get(opt));
//			}
//			if(columnSet.size() == 1) {
//				String result = columnSet.iterator().next();
//				if(!result.equals("error") &&
//						!result.equals("timeout") &&
//						!result.equals("sanitize") &&
//						!result.equals("excep")) {
//					return true;
//				}
//			}
//		}
//
//		return false;
//	}
	
//	public boolean searchResultLabel(List<CompilationInfo> CompilationInfoList, String resultLabel) {
//		if(CompilationInfoList.size() == 0) return false;
//
//		for(CompilationInfo info: CompilationInfoList) {
//			for(String result: info.getOutputChecksumMap().values()) {
//				if(result.equals(resultLabel)) {
//					return true;
//				}
//			}
//		}
//		return false;
//	}

//	public boolean parseColumn(List<CompilationInfo> CompilationInfoList, String resultType) {
//		if(CompilationInfoList.isEmpty()) return false;
//
//		List<String> optList = CompilationInfoList.get(0).getOutputChecksumMap().keySet().stream().toList();
//		for(String opt: optList) {
//			Set<String> columnSet = new HashSet<String>();
//			for (CompilationInfo info : CompilationInfoList) {
//				columnSet.add(info.getOutputChecksumMap().get(opt));
//			}
//			if(columnSet.size() > 1 && columnSet.contains(resultType)){
//				return true;
//			}
//		}
//
//		return false;
//	}
	
//	public List<String> parse(List<CompilationInfo> CompilationInfoList){
//		List<String> res = new ArrayList<String>();
//
////		if(parseChecksum(CompilationInfoList)){
////			res.add("checksum");
////		}
//		if(parseError(CompilationInfoList)){
//			res.add("error");
//		}
//		if(parsePerformance(CompilationInfoList)){
//			res.add("performance");
//		}
//		if(res.isEmpty()){
//			res.add("correct");
//		}
//		return res;
//	}

//	public boolean parseChecksum2(List<CompilationInfo> CompilationInfoList){
//		Set<String> checksumSet = new HashSet<String>();
//		for(CompilationInfo info: CompilationInfoList){
//			for(String checksum: info.getOutputChecksumMap().values()){
//				if(checksum.equals("error")) continue;
//				if(checksum.equals("exception")) continue;
//				if(checksum.equals("timeout")) continue;
//				checksumSet.add(checksum);
//				if(checksumSet.size() > 1) return true;
//			}
//		}
//		return false;
//	}

//	public boolean parseError(List<CompilationInfo> CompilationInfoList){
//		List<String> optList = CompilationInfoList.get(0).getOutputChecksumMap().keySet().stream().toList();
//		for(String opt: optList) {
//			Set<String> columnSet = new HashSet<String>();
//			for (CompilationInfo info : CompilationInfoList) {
//				columnSet.add(info.getOutputChecksumMap().get(opt));
//			}
//			if(columnSet.size() > 1 && columnSet.contains("exception")){
//				return true;
//			}
//		}
//
//		return false;
//	}
//
//	public boolean parsePerformance(List<CompilationInfo> CompilationInfoList){
//
//		return false;
//	}

}
