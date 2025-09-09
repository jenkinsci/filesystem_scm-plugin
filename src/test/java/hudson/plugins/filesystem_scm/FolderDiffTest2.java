package hudson.plugins.filesystem_scm;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FolderDiffTest2 {

    @Test
    void GetRelative_correctFileAndFolder_Subpath() throws IOException {
        String folder = TestUtils.createPlatformDependentPath("c:", "tmp");
        String expected = TestUtils.createPlatformDependentPath("abc", "qq", "qq.java");
        String actual = FolderDiff.getRelativeName(TestUtils.createPlatformDependentPath(folder, expected), folder);
        assertEquals(expected, actual);
    }

    @Test
    void GetRelative_correctFileAndDrive_Subpath() throws IOException {
        String folder = TestUtils.createPlatformDependentPath("c:");
        String expected = TestUtils.createPlatformDependentPath("tmp", "abc", "qq", "qq.java");
        String actual = FolderDiff.getRelativeName(TestUtils.createPlatformDependentPath(folder, expected), folder);
        assertEquals(expected, actual);
    }

    @Test
    void GetRelative_FileAndFolderWithALetterMissingInName_Exception() {
        String folder = TestUtils.createPlatformDependentPath("c:", "tm");
        assertThrows(IOException.class, () -> FolderDiff
                .getRelativeName(TestUtils.createPlatformDependentPath("c:", "tmp", "abc", "qq", "qq.java"), folder));
    }

    @Test
    void GetRelative_FileAndFolderNotParentOfFile_Exception() {
        String folder = TestUtils.createPlatformDependentPath("c:", "def");
        String file = TestUtils.createPlatformDependentPath("c:", "tmp", "abc", "qq", "qq.java");
        assertThrows(IOException.class, () -> FolderDiff.getRelativeName(file, folder));
    }

    @Test
    void GetRelative_FileAndFolderWithPathSeperatorAppended_Exception() {
        String folder = TestUtils.createPlatformDependentPath("c:", "tmp");
        String expected = TestUtils.createPlatformDependentPath("abc", "qq", "qq.java");
        assertThrows(IOException.class, () -> FolderDiff.getRelativeName(TestUtils.createPlatformDependentPath(folder, expected),
                folder.concat("//")));
    }
}
