package common;

import AST_Information.Inform_Gen.AstInform_Gen;
import AST_Information.model.FieldVar;
import AST_Information.model.StructUnionBlock;
import ObjectOperation.file.FileModify;
import processtimer.ProcessTerminal;
import utity.AvailableVariable;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonInfoFromFile {

    public static int getFuncStartLine(List<String> initialFileList){
        int count = 0;
        int lineNumber = 0;
        for(String s: initialFileList){
            count++;
            if(s.replaceAll("\\/\\*.+\\*\\/", "").trim().matches("(\\s*\\w+\\s+)+func_\\d+\\s*\\(void\\)\\s*\\{")) {
                lineNumber = count;
                break;
            }
        }
        return lineNumber;
    }

    public static boolean checkCorrection(File file){
        ExceptionCheck ec = new ExceptionCheck();
        return ec.filterUB(file);
    }

    public static boolean isBit(String complexType, String fieldName, AstInform_Gen astgen){
        for(StructUnionBlock su: astgen.allStructUnionMap.values()) {
            if((su.getBlockType() + " " + su.getName()).equals(complexType)){
                for(FieldVar field: su.getChildField()) {
                    if(field.getName().equals(fieldName))
                        return field.getIsBit();
                }
            }
        }
        return false;
    }

    public static String getComplexType(String varValue, List<AvailableVariable> structUnionAvarList){
        for(AvailableVariable avar: structUnionAvarList){
            if(avar.getValue().equals(varValue))
                return avar.getType();
        }
        return null;
    }

    public static String replaceRegex(String initial){
        return initial.replaceAll("\\[", "\\\\[")
                .replaceAll("\\(", "\\\\(")
                .replaceAll("\\)", "\\\\)")
                .replaceAll("\\*", "\\\\*")
                .replaceAll("\\.", "\\\\.")
                .replaceAll("\\&", "\\\\&")
                .replaceAll("\\+", "\\\\+");
    }

    public static void cpOriginalFile(File muDir, File file){
        if (muDir.exists() && muDir.isDirectory()) {
            FileModify fm = new FileModify();
            fm.deleteFolder(muDir);
        }
        muDir.mkdirs();
        ProcessTerminal.voidNotMemCheck("cp " + file.getAbsolutePath() + " " + muDir.getAbsolutePath(), "sh");
    }

    public static boolean isHaveOp(String line){
        return line.contains("+=") || line.contains("-=") ||
                line.contains("^=") || line.contains("&=") ||
                line.contains("*=") || line.contains(">>=") ||
                line.contains("<<=") || line.contains("/=") ||
                line.contains("%=") ||line.contains("|=");
    }

    public static boolean containsAssignmentOperator(String line) {
        // 正则表达式：匹配 '=' 但不匹配 '>=', '<=', '!=', '=='
        String regex = "(?<![<>!=])=(?![>=!=])";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(line);
        return matcher.find(); // 如果找到匹配项，则返回 true
    }

}
