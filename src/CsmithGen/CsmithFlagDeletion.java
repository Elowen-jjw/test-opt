package CsmithGen;

import common.ExceptionCheck;
import ObjectOperation.file.FileModify;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CsmithFlagDeletion {

    final static HashMap<String, String> typeMap = new HashMap<>() {{
        put("int8_t", "char");
        put("uint8_t", "unsigned char");
        put("int16_t", "short");
        put("uint16_t", "unsigned short");
        put("int32_t", "int");
        put("uint32_t", "unsigned int");
        put("int64_t", "long");
        put("uint64_t", "unsigned long");
    }};

    public boolean run(File file){
        deleteComment(file);
        deleteCsmithFlag(file);
        FileModify.formatFile(file);
        System.out.println("Start to check csmith deletion correction.......");
        return checkCorrection(file);
    }

    public void deleteComment(File file){
        CFormat format = new CFormat();
        format.dealComment(file);
    }

    public void deleteCsmithFlag(File file){
        FileModify fm = new FileModify();
        List<String> initialList = fm.readFile(file);
        List<String> endList = new ArrayList<>();

        boolean isStartMain = false;
        for(String line: initialList){
            //1. delete csmith.h
            if(line.trim().equals("#include \"csmith.h\"")) {
                endList.add("#include<stdio.h>");
                endList.add("#include<stdlib.h>");
                endList.add("#include<math.h>");
                endList.add("#include<assert.h>");
                endList.add("#include<limits.h>");
                endList.add("#include<stdint.h>");
                endList.add("#include \"safe_math_macros.h\"");
                continue;
            }

            //2. deal with ijklmno declared individually
            Pattern p_overall = Pattern.compile("\\s*int\\s+([ijklmno][0-9]+,\\s*)*[ijklmno][0-9]+;");
            Matcher m_overall = p_overall.matcher(line.trim());
            if(m_overall.matches()){
                Pattern p_single = Pattern.compile("[ijklmno][0-9]+");
                Matcher m_single = p_single.matcher(line.trim());
                StringBuilder sb = new StringBuilder();
                while(m_single.find()){
                    sb.append("int ").append(m_single.group()).append(";\n");
                }
                endList.add(sb.toString());
                continue;
            }

            //3. delete printf in main function
//            if(line.trim().startsWith("printf(\"checksum")){
//                continue;
//            }

            //4. delete const variable declare
            if(isHaveConstDeclare(line)){
                endList.add(line.replaceAll("const", ""));
                continue;
            }

            //5. delete int ijk in main function
            if(line.trim().equals("int main(void) {")){
                isStartMain = true;
            }

            if(isStartMain && line.trim().matches("int\\s+[ijklmno][0-9]+\\s*;")) continue;

//            else if(line.trim().equals("static long __undefined;")){
//                continue;
//            }
//            line = typeReplaced(line);

            endList.add(line);
        }
        fm.writeFile(file, endList);
    }

    public boolean isHaveConstDeclare(String line){
        if(line.trim().length() == 0) return false;
        String[] words = line.trim().split("\\s+");
        for(String word: words){
            if(word.matches("\\**const"))
                return true;
        }
        return false;
    }

    public String typeReplaced(String searchString) {
        for (String s : typeMap.keySet()) {
//            Pattern pattern = Pattern.compile("[^_]" + (s) + "[^_]");
//            Matcher matcher = pattern.matcher(searchString);
//            while(matcher.find()){
                searchString = searchString.replaceAll("(?<!_)" + s + "(?!_)", typeMap.get(s));
//            }
        }
        return searchString;
    }

    public boolean checkCorrection(File file){
        ExceptionCheck ec = new ExceptionCheck();
        return ec.filterUB(file);
    }
}
