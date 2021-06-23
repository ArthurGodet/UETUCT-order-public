/*
@author Arthur Godet <arth.godet@gmail.com>
@since 07/03/2019
*/
package data;

import com.fasterxml.jackson.databind.ObjectMapper;
import gnu.trove.list.array.TIntArrayList;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class Factory {

    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////// UTILS /////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    public static <T> T fromFile(String path, Class<T> valueType) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(new File(path), valueType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void toFile(String path, Object toWrite) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File f = new File(path);
            if (!f.getParentFile().exists()) {
                f.getParentFile().mkdirs();
            }
            String s = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toWrite);
            FileWriter fw = new FileWriter(path);
            fw.write(s);
            fw.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private static void addFiles(List<File> list, File file, String fileType, boolean includeSubFolders) {
        if (file.isDirectory() && includeSubFolders) {
            for (File f : file.listFiles()) {
                addFiles(list, f, fileType, true);
            }
        } else if (file.getName().contains(fileType)) {
            list.add(file);
        }
    }

    public static File[] listAllFiles(String folderPath, String fileType, boolean includeSubFolders) {
        List<File> list = new LinkedList<>();
        File folder = new File(folderPath);
        for (File f : folder.listFiles()) {
            addFiles(list, f, fileType, includeSubFolders);
        }
        return list.toArray(new File[list.size()]);
    }

    public static boolean contains(int[] array, int value) {
        for(int i = 0; i<array.length; i++) {
            if(value == array[i]) {
                return true;
            }
        }
        return false;
    }
}
