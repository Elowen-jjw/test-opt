package objectoperation.datatype;

public class UnsignedOperation {
	
	//将data字节型数据转换为0~255 (0xFF 即BYTE)。
	public static int getUnsignedByte (char data){     
        return data&0x0FF;
    }
	
	//将data字节型数据转换为0~65535 (0xFFFF 即 WORD)。
	public static int getUnsignedByte (short data){      
           return data&0x0FFFF;
    }   
	
	//将int数据转换为0~4294967295 (0xFFFFFFFF即DWORD)。
    public static long getUnsignedInt (int data){     
        return data&0x0FFFFFFFFl;
    }
    
    public static long getUnsignedLong (long data){     
        return data&0x0FFFFFFFFFFFFFFFFl;
    }
}
