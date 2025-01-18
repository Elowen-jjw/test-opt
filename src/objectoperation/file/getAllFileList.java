package objectoperation.file;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class getAllFileList {
	
	public getAllFileList() {
		
	}
	
	public getAllFileList( File dir ){
		if (!dir.exists()) {
            System.out.println(dir.toString()+" don't exist!");
            return;
        }
	} 

    public void getAllFile(File fileInput, List<File> allFileList) {
        
        File[] fileList = fileInput.listFiles();
        assert fileList != null;
        for (File file : fileList) {
            if (file.isDirectory()) {
                getAllFile(file, allFileList);
            } else {             
                allFileList.add(file);
            }
        }
        
    }
    
    public void compareFileList(List<File> allFileList) {
        Collections.sort(allFileList, new Comparator< File>() {
            @Override
            public int compare(File o1, File o2) {
                if (o1.isDirectory() && o2.isFile())
                    return -1;
                if (o1.isFile() && o2.isDirectory())
                    return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });
    }
    
    
}
