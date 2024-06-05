package mtk.apix.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

/**
 * @author mahatoky rasolonirina
 */
public final class ApixJson {

    private ApixJson() {
    }

    public static <T> T get(String filePath, Class<T> objectClass) throws IOException {
        return new ObjectMapper().readValue(new File(filePath), objectClass);
    }

}
