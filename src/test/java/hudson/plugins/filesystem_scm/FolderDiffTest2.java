package hudson.plugins.filesystem_scm;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;;

public class FolderDiffTest2 {

    @Test
    public void GetRelative_correctFileAndFolder_Subpath() throws IOException {
        String folder = TestUtils.createPlatformDependendPath("c:", "tmp");
        String expected = TestUtils.createPlatformDependendPath("abc", "qq", "qq.java");
        String actual = FolderDiff.getRelativeName(TestUtils.createPlatformDependendPath(folder, expected), folder);
        assertEquals(expected, actual);
    }

    @Test
    public void GetRelative_correctFileAndDrive_Subpath() throws IOException {
        String folder = TestUtils.createPlatformDependendPath("c:");
        String expected = TestUtils.createPlatformDependendPath("tmp", "abc", "qq", "qq.java");
        String actual = FolderDiff.getRelativeName(TestUtils.createPlatformDependendPath(folder, expected), folder);
        assertEquals(expected, actual);
    }

    @Test(expected = IOException.class)
    public void GetRelative_FileAndFolderWithALetterMissingInName_Exception() throws IOException {
        String folder = TestUtils.createPlatformDependendPath("c:", "tm");
        String actual = FolderDiff
                .getRelativeName(TestUtils.createPlatformDependendPath("c:", "tmp", "abc", "qq", "qq.java"), folder);
    }

    @Test(expected = IOException.class)
    public void GetRelative_FileAndFolderNotParentOfFile_Exception() throws IOException {
        String folder = TestUtils.createPlatformDependendPath("c:", "def");
        String file = TestUtils.createPlatformDependendPath("c:", "tmp", "abc", "qq", "qq.java");
        String x = FolderDiff.getRelativeName(file, folder);
    }

    @Test(expected = IOException.class)
    public void GetRelative_FileAndFolderWithPathSeperatorAppended_Exception() throws IOException {
        String folder = TestUtils.createPlatformDependendPath("c:", "tmp");
        String expected = TestUtils.createPlatformDependendPath("abc", "qq", "qq.java");
        String actual = FolderDiff.getRelativeName(TestUtils.createPlatformDependendPath(folder, expected),
                folder.concat("//"));
    }
}
