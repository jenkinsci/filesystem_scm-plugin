package hudson.plugins.filesystem_scm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;


;

public class FolderDiffTest2 {

    @Test
    void GetRelative_correctFileAndFolder_Subpath() throws IOException {
        String folder = TestUtils.createPlatformDependendPath("c:", "tmp");
        String expected = TestUtils.createPlatformDependendPath("abc", "qq", "qq.java");
        String actual = FolderDiff.getRelativeName(TestUtils.createPlatformDependendPath(folder, expected), folder);
        assertEquals(expected, actual);
    }

    @Test
    void GetRelative_correctFileAndDrive_Subpath() throws IOException {
        String folder = TestUtils.createPlatformDependendPath("c:");
        String expected = TestUtils.createPlatformDependendPath("tmp", "abc", "qq", "qq.java");
        String actual = FolderDiff.getRelativeName(TestUtils.createPlatformDependendPath(folder, expected), folder);
        assertEquals(expected, actual);
    }

    @Test
    void GetRelative_FileAndFolderWithALetterMissingInName_Exception()throws Exception {
        String folder = TestUtils.createPlatformDependendPath("c:", "tm");
        assertThrows(IOException.class, () -> {
            String actual = FolderDiff
                .getRelativeName(TestUtils.createPlatformDependendPath("c:", "tmp", "abc", "qq", "qq.java"), folder);
        });
    }

    @Test
    void GetRelative_FileAndFolderNotParentOfFile_Exception()throws Exception {
        String folder = TestUtils.createPlatformDependendPath("c:", "def");
        String file = TestUtils.createPlatformDependendPath("c:", "tmp", "abc", "qq", "qq.java");
        assertThrows(IOException.class, () -> {
            String x = FolderDiff.getRelativeName(file, folder);
        });
    }

    @Test
    void GetRelative_FileAndFolderWithPathSeperatorAppended_Exception()throws Exception {
        String folder = TestUtils.createPlatformDependendPath("c:", "tmp");
        String expected = TestUtils.createPlatformDependendPath("abc", "qq", "qq.java");
        assertThrows(IOException.class, () -> {
            String actual = FolderDiff.getRelativeName(TestUtils.createPlatformDependendPath(folder, expected),
                folder.concat("//"));
        });
    }
}
