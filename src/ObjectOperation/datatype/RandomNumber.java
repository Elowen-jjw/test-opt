package ObjectOperation.datatype;

import java.text.DecimalFormat;
import java.util.Random;

public class RandomNumber {
    public String getRandomNum(String type) {
        Random random = new Random();
        DecimalFormat dfLong = new DecimalFormat("#0");
        if(type.equals("char") || type.equals("int8_t")) {
            return String.valueOf(random.nextInt(-(int)Math.pow(2, 7),(int)Math.pow(2, 7)));
        }
        else if(type.equals("unsigned char") || type.equals("uint8_t")) {
            return String.valueOf(random.nextInt((int)Math.pow(2, 8)));
        }
        else if(type.equals("short") || type.equals("int16_t")) {
            return String.valueOf(random.nextInt(-(int)Math.pow(2, 15),(int)Math.pow(2, 15)));
        }
        else if(type.equals("unsigned short") || type.equals("uint16_t")) {
            return String.valueOf(random.nextInt((int)Math.pow(2, 16)));
        }
        else if(type.equals("int") || type.equals("int32_t")) {
            return String.valueOf(random.nextInt(Integer.MIN_VALUE,Integer.MAX_VALUE));
        }
        else if(type.equals("unsigned int") || type.equals("uint32_t")) {
            return String.valueOf(random.nextLong(0,(long)Math.pow(2, 31)));
        }
        else if(type.equals("long") || type.equals("int64_t")) {
            return String.valueOf(random.nextLong(Long.MIN_VALUE,Long.MAX_VALUE));
        }
        else if(type.equals("unsigned long") || type.equals("uint64_t")) {
            return String.valueOf(dfLong.format(random.nextDouble(0, Math.pow(2, 63))));
        }
        return null;
    }
    public String getRandomNumHasMin(String type, String minValue) {
        Random random = new Random();
        DecimalFormat dfLong = new DecimalFormat("#0");
        if(type.equals("char") || type.equals("int8_t")) {
            return String.valueOf(random.nextInt(Integer.valueOf(minValue),(int)Math.pow(2, 7)));
        }
        else if(type.equals("unsigned char") || type.equals("uint8_t")) {
            return String.valueOf(random.nextInt(Integer.valueOf(minValue),(int)Math.pow(2, 8)));
        }
        else if(type.equals("short") || type.equals("int16_t")) {
            return String.valueOf(random.nextInt(Integer.valueOf(minValue),(int)Math.pow(2, 15)));
        }
        else if(type.equals("unsigned short") || type.equals("uint16_t")) {
            return String.valueOf(random.nextInt(Integer.valueOf(minValue), (int)Math.pow(2, 16)));
        }
        else if(type.equals("int") || type.equals("int32_t")) {
            return String.valueOf(random.nextInt(Integer.valueOf(minValue),Integer.MAX_VALUE));
        }
        else if(type.equals("unsigned int") || type.equals("uint32_t")) {
            return String.valueOf(random.nextLong(Long.valueOf(minValue),(long)Math.pow(2, 31)));
        }
        else if(type.equals("long") || type.equals("int64_t")) {
            return String.valueOf(random.nextLong(Long.valueOf(minValue),Long.MAX_VALUE));
        }
        else if(type.equals("unsigned long") || type.equals("uint64_t")) {
            return String.valueOf(dfLong.format(random.nextDouble(Math.abs(Double.valueOf(minValue)), Math.pow(2, 63))));
        }
        return null;
    }

}
