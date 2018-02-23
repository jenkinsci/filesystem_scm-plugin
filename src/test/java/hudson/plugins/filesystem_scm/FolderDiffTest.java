package hudson.plugins.filesystem_scm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import hudson.plugins.filesystem_scm.FolderDiff.Entry;

public class FolderDiffTest {

    File src1, dst1;
    @Rule
    public TemporaryFolder srcFolder1 = new TemporaryFolder();
    @Rule
    public TemporaryFolder dstFolder1 = new TemporaryFolder();
    String rootFilePath, subfolderFilePath, folderFilePath;

    @Before
    public void setupSrcAndDst() throws IOException {
        // setup src Folder
        src1 = srcFolder1.getRoot();
        src1.createNewFile();
        subfolderFilePath = createFile(src1, "Folder", "subFolder", "subFolderFile.txt");
        folderFilePath = createFile(src1, "Folder", "FolderFile.git");
        rootFilePath = createFile(src1, "RootFile.java");

        // setup destination Folder
        dst1 = dstFolder1.getRoot();
        dst1.createNewFile();
        createFile(dst1, subfolderFilePath);
        createFile(dst1, folderFilePath);
        createFile(dst1, rootFilePath);

    }

    private String createFile(File root, String... strings) throws IOException {
        String path = TestUtils.createPlatformDependendPath(strings);
        File file = new File(root, path);
        System.out.println(file.getAbsolutePath());
        FileUtils.touch(file);
        Assert.assertTrue(file.exists());
        return path;
    }

    @Test
    public void getFiles2Delete_noRemovedSrcFiles_nothing2Delete() throws IOException, InterruptedException {
        assertMarkAsDelete(new HashSet<FolderDiff.Entry>(), src1, dst1);
    }

    @Test
    public void getFiles2Delete_allDestinationFolderDeleted_nothing2Delete() throws IOException, InterruptedException {
        FileUtils.deleteDirectory(dst1);
        assertMarkAsDelete(new HashSet<FolderDiff.Entry>(), src1, dst1);
    }

    @Test
    public void getFiles2Delete_aSourceFolderDeleted_markAllFilesForDeletion()
            throws IOException, InterruptedException {
        FileUtils.deleteDirectory(src1);
        Set<FolderDiff.Entry> expected = new HashSet<FolderDiff.Entry>();
        expected.add(new Entry(folderFilePath, FolderDiff.Entry.Type.DELETED));
        expected.add(new Entry(rootFilePath, FolderDiff.Entry.Type.DELETED));
        expected.add(new Entry(subfolderFilePath, FolderDiff.Entry.Type.DELETED));
        assertMarkAsDelete(expected, src1, dst1);
    }

    @Test
    public void getFilesNewOrModifiedFiles_noNewOrModifiedFilesLastBuildTimeNow_nothing2Add() throws IOException {
        long lastBuildTime = System.currentTimeMillis();
        assertMarkAsNewOrModified(new HashSet<FolderDiff.Entry>(), lastBuildTime, src1, dst1);
    }

    @Test
    public void getFilesNewOrModifiedFiles_DestinationFolderDeleted_AllNewFiles() throws IOException {
        FileUtils.deleteDirectory(dst1);
        Set<FolderDiff.Entry> expected = new HashSet<FolderDiff.Entry>();
        expected.add(new Entry(folderFilePath, FolderDiff.Entry.Type.NEW));
        expected.add(new Entry(rootFilePath, FolderDiff.Entry.Type.NEW));
        expected.add(new Entry(subfolderFilePath, FolderDiff.Entry.Type.NEW));
        assertMarkAsNewOrModified(expected, 0l, src1, dst1);
    }

    @Test
    public void getFilesNewOrModifiedFiles_SourceFolderDeleted_noNewFiles() throws IOException {
        FileUtils.deleteDirectory(src1);
        assertMarkAsNewOrModified(new HashSet<FolderDiff.Entry>(), 0l, src1, dst1);
    }

    @Test
    public void getFilesNewOrModifiedFiles_SourceFileModificationDateNewerThenLastBuildTime_AddAllModifiedFiles()
            throws IOException {
        long lastBuildTime = System.currentTimeMillis() - 1000 * 60; // lastBuild Time 1min in past -> src Files
                                                                     // modified
        Set<FolderDiff.Entry> expected = new HashSet<FolderDiff.Entry>();
        expected.add(new Entry(folderFilePath, FolderDiff.Entry.Type.MODIFIED));
        expected.add(new Entry(rootFilePath, FolderDiff.Entry.Type.MODIFIED));
        expected.add(new Entry(subfolderFilePath, FolderDiff.Entry.Type.MODIFIED));
        assertMarkAsNewOrModified(expected, lastBuildTime, src1, dst1);
    }

    @Test
    public void getFilesNewOrModifiedFiles_OneSourceFileNewerThanDestinationFile_SourceFileModified()
            throws IOException {
        Set<FolderDiff.Entry> expected = new HashSet<FolderDiff.Entry>();
        expected.add(new Entry(folderFilePath, FolderDiff.Entry.Type.MODIFIED));
        long lastBuildTime = System.currentTimeMillis();
        // implementation works only when times between file modification dates are at
        // least different by 1000 mys
        // setModification Time in the future -> therfore the destination file needs to
        // be updated
        Assert.assertTrue((new File(src1, folderFilePath)).setLastModified(lastBuildTime + 1000));
        assertMarkAsNewOrModified(expected, lastBuildTime, src1, dst1);
    }

    private void assertMarkAsNewOrModified(Set<FolderDiff.Entry> expected, long time, File src, File dst) {
        FolderDiffFake diff = getFolderDiff(src, dst);
        List<FolderDiff.Entry> result = diff.getNewOrModifiedFiles(time, false);
        assertEquals(expected, new HashSet<FolderDiff.Entry>(result));
        Assert.assertEquals(expected.size(), diff.copyFilePairs.size());
    }

    private void assertMarkAsDelete(Set<FolderDiff.Entry> expected, File src, File dst) throws InterruptedException {
        FolderDiffFake diff = getFolderDiff(src, dst);
        List<FolderDiff.Entry> result = diff.getFiles2Delete(false);
        assertEquals(expected, new HashSet<FolderDiff.Entry>(result));
        Assert.assertEquals(expected.size(), diff.deleteFiles.size());
    }

    private FolderDiffFake getFolderDiff(File src, File dst) {
        return new FolderDiffFake(src.getAbsolutePath(), dst.getAbsolutePath());
    }
}