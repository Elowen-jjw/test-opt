package csmithgen;

import processmemory.ProcessCompilerPerformace;

import java.io.File;
import java.util.List;
import java.util.Random;

public class SwarmGen {
    public final static String fixedConfig = "--no-argc --no-safe-math --check-global " +
            "--max-funcs 4 --max-block-depth 4 --max-block-size 5 --max-expr-complexity 5 " +
            "--max-pointer-depth 4 " +
            "--max-array-dim 3 --max-array-len-per-dim 10 " +
            "--partial-expand for " +
            "--paranoid --binary-constant " +
            // --stop-by-stmt 100 --inline-function --function-attributes --type-attributes --variable-attributes --compiler-attributes --enable-access-once --fresh-array-ctrl-var-names --random-random
            "--quiet";
    public final static String tools = "csmith";
    public final static String writeCommand = " > random";
    public final static String fileSuffix = ".c";

    String randomIndexDir = "";
    public SwarmGen(String randomIndexDir){
        this.randomIndexDir = randomIndexDir;
    }

    public String genRandomSpecificConfig(){
        Random random = new Random();
//        String commaOp = random.nextBoolean()? "": "--no-comma-operators ";
//        String compoundAssign = random.nextBoolean()? "": "--no-compound-assignment ";
//        String embeddedAssign = random.nextBoolean()? "": "--no-embedded-assigns ";
//        String autoInOrDecre = random.nextBoolean()? "": "--no-pre-incr-operator --no-pre-decr-operator --no-post-incr-operator --no-post-decr-operator ";
//        String div= random.nextBoolean()? "": "--no-divs ";
//        String mul = random.nextBoolean()? "": "--no-muls ";
//        String longlong = random.nextBoolean()? "": "--no-longlong ";
//        String math64 = random.nextBoolean()? "": "--no-math64 ";
//        String struct = random.nextBoolean()? "": "--no-structs ";
//        String bitfield = random.nextBoolean()? "": "--no-bitfields ";
//        String packedStruct = random.nextBoolean()? "": "--no-packed-struct ";
//        String union = random.nextBoolean()? "": "--no-unions ";
//        String array = random.nextBoolean()? "": "--no-arrays ";
//        String pointer = random.nextBoolean()? "": "--no-pointers ";
//        String constOb = random.nextBoolean()? "": "--no-consts ";
//        String constPointer = random.nextBoolean()? "": "--no-const-pointers ";
//        String volatileOb = random.nextBoolean()? "": "--no-volatiles ";
//        String volatilePointer = random.nextBoolean()? "": "--no-volatile-pointers ";
//        String inlineFuc = random.nextBoolean()? "--inline-function --inline-function-prob 50 ": "--no-inline-function ";
//        String paranoid = random.nextBoolean()? "--paranoid ": "";
//        String binaryConst = random.nextBoolean()? "--binary-constant ": "";
////        String builtin = random.nextBoolean()? "--builtins": "--no-builtins";
//        String funcAttribute = random.nextBoolean()? "--function-attributes ": "";
//        String typeAttribute = random.nextBoolean()? "--type-attributes ": "";
//        String varAttribute = random.nextBoolean()? "--variable-attributes ": "";
        //extensions
//        String atol = random.nextBoolean()?"--addr-taken-of-locals ": "--no-addr-taken-of-locals ";
        String rac = random.nextBoolean()?"--strict-const-arrays ": "";
        String svr = random.nextBoolean()? "--strict-volatile-rule ": "";
//        String cac = random.nextBoolean()? "--const-as-condition ": "";
//        String meq = random.nextBoolean()? "--match-exact-qualifiers ": "";
        String nfgs = random.nextBoolean()? "--no-force-globals-static --no-inline-function ": "--inline-function --inline-function-prob 50 ";
        String nfnua = random.nextBoolean()? "--no-force-non-uniform-arrays ": "";
        return " "
//                + commaOp  + compoundAssign  + embeddedAssign + autoInOrDecre + div + mul
//                + longlong  + math64 + struct + bitfield + packedStruct
//                + union  + array + pointer + constOb + constPointer + volatileOb
//                + volatilePointer  + inlineFuc + paranoid  + binaryConst
//                + funcAttribute  + typeAttribute  + varAttribute
//                + atol
                + rac
                + svr
//                + cac
//                + meq
                + nfgs
                + nfnua
                ;
    }

    public File genRandomTestcase(int randomIndex){
        while(true) {
            String specificConfigs = genRandomSpecificConfig();
            String genRandomCommand = "cd " + randomIndexDir + " && " +
                    SwarmGen.tools + specificConfigs + SwarmGen.fixedConfig + SwarmGen.writeCommand + randomIndex + SwarmGen.fileSuffix;
            List<String> execList = ProcessCompilerPerformace.process(genRandomCommand, 10, "sh", "csmithgen");
            if(!execList.isEmpty() && execList.get(0).equals("timeout")){
                System.out.println("generate csmith timeout");
                continue;
            }

            File randomFile = new File(randomIndexDir + "/" + "random" + randomIndex + SwarmGen.fileSuffix);
            ExceptionCheck ec = new ExceptionCheck();
            System.out.println(genRandomCommand);
            if ((randomFile.length()/1024 > 10) && (randomFile.length()/1024 < 30)){
                System.out.println("passed randomFile size: " + randomFile.length()/1024);
                if(!ec.isNotHaveLoop(randomFile)){
                    if(!ec.isTimeout(randomFile)){
                        if(!ec.filterUB(randomFile)) {
                            System.out.println("new config..........");
                            Format format = new Format();
                            format.addBrace(randomFile);
                            format.format(randomFile);
                            return randomFile;
                        }else{
                            System.out.println("this file has sanitizer error... ");
                            randomFile.delete();
                        }
                    }else{
                        System.out.println("this file timeout... ");
                        randomFile.delete();
                    }
                } else{
                    System.out.println("this file don not have loop... ");
                    randomFile.delete();
                }
            } else {
                System.out.println("randomFile size: " + randomFile.length()/1024);
                randomFile.delete();
            }
        }
    }
}
