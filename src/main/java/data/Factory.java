/*
@author Arthur Godet <arth.godet@gmail.com>
@since 07/03/2019
*/
package data;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class Factory {
    /**
     * Reads an Object from a JSON file.
     *
     * @param path the path of the JSON file
     * @param valueType the class of the Object to read
     * @param <T> the generic class
     * @return the Object in the JSON file
     */
    public static <T> T fromFile(String path, Class<T> valueType) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(new File(path), valueType);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns true if the array contains the value.
     *
     * @param array the array
     * @param value the value
     * @return true iff the array contains the value
     */
    public static boolean contains(int[] array, int value) {
        for(int i = 0; i<array.length; i++) {
            if(value == array[i]) {
                return true;
            }
        }
        return false;
    }
}
