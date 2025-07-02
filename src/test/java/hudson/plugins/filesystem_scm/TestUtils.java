package hudson.plugins.filesystem_scm;

import java.io.File;

public class TestUtils {

    public static String createPlatformDependentPath(String... parts) {
        String path = "";
        for (int i = 0; i < parts.length - 1; i++) {
            path = path.concat(parts[i]).concat(File.separator);
        }
        path = path.concat(parts[parts.length - 1]);
        return path;
    }
}
