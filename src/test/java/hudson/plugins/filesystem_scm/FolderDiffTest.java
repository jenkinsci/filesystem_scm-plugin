package hudson.plugins.filesystem_scm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import hudson.plugins.filesystem_scm.FolderDiff.Entry;

public class FolderDiffTest {

    File src, src1;
    File dst, dst1;
    @Rule
    public TemporaryFolder srcFolder = new TemporaryFolder();
    @Rule
    public TemporaryFolder dstFolder = new TemporaryFolder();
    @Rule
    public TemporaryFolder srcFolder1 = new TemporaryFolder();
    @Rule
    public TemporaryFolder dstFolder1 = new TemporaryFolder();
    long modifiedTime;
    long checkTime;
    final static long diff = 60 * 60 * 2 * 1000L;
    String rootFilePath, subfolderFilePath, folderFilePath;

    @Before
    public void setupSrcAndDst() throws IOException {
        src = srcFolder.getRoot();
        for (int i = 0; i < 50; i++)
            createRandomFile(src);
        dst = dstFolder.getRoot();
        FileUtils.copyDirectory(src, dst);

        checkTime = System.currentTimeMillis() - 60 * 60 * 1000L;
        modifiedTime = System.currentTimeMillis() - diff;

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

    private static File createRandomFile(File dir) {
        while (true) {
            String name = getRandomName();
            File tmp = new File(dir, name);
            if (!tmp.exists()) {
                try {
                    FileUtils.touch(tmp);
                    tmp.setLastModified(System.currentTimeMillis() - 2 * diff);
                    System.out.println(tmp.getAbsolutePath());
                    return tmp;
                } catch (IOException e) {
                    continue;
                }
            }
        }
    }

    private static String getRandomName() {
        String[] names = { "Apple", "Banana", "Cherry", "Durian", "Eggfruit", "Figs", "Grapes", "Honeydew", "Melon",
                "Jujube", "Kiwi", "Lemon", "Mango", "Nectarine", "Orange", "Pineapple", "Raisins", "Strawberries",
                "Tangerine", "Watermelon" };
        String[] exts = { "cpp", "jsp", "java", "tmp", "$$$", "txt", "xml", "exe", "dll", "", "html", "css", "js" };

        StringBuffer buf = new StringBuffer();
        buf.append(names[new Random().nextInt(names.length)]);
        int count = new Random().nextInt(4);
        for (int i = 0; i < count; i++) {
            if (new Random().nextInt(3) == 0)
                buf.append("/");
            buf.append(names[new Random().nextInt(names.length)]);
        }
        String name = buf.toString();

        String ext = exts[new Random().nextInt(exts.length)];

        if (0 == ext.length())
            return name;
        else
            return name + "." + ext;
    }

    @Test
    public void getNewOrModifiedFiles_noChanges_EmptyList() {
        FolderDiff diff = getFolderDiff(src, dst);
        List<FolderDiff.Entry> result = diff.getNewOrModifiedFiles(checkTime, false);
        assertTrue(result.isEmpty());
        ChangelogSet changelogSet = new ChangelogSet(null, result);
        assertTrue(changelogSet.isEmptySet());
    }

    @Test
    public void getNewOrModifiedFiles_someModified_AllFound() throws IOException {
        Set<FolderDiff.Entry> expected = createFilesAndExpectations(src, new FileCallable() {
            public void process(File file, Set<FolderDiff.Entry> expected) throws IOException {
                boolean modified = file.setLastModified(modifiedTime);
                if (!modified) {
                    throw new IOException("setlastModified failed");
                }
                String relativeName = FolderDiff.getRelativeName(file.getAbsolutePath(), src.getAbsolutePath());
                expected.add(new FolderDiff.Entry(relativeName, FolderDiff.Entry.Type.MODIFIED));
            }
        });
        assertMarkAsNewOrModified(expected, src, dst);
    }

    @Test
    public void getNewOrModifiedFiles_allCopied_AllFound() throws IOException {
        Set<FolderDiff.Entry> expected = createFilesAndExpectations(src,
                new FileCallableImpl(true, FolderDiff.Entry.Type.NEW));
        assertMarkAsNewOrModified(expected, src, dst);
    }

    @Test
    public void getNewOrModifiedFiles_allCreated_AllFound() throws IOException {
        Set<FolderDiff.Entry> expected = createFilesAndExpectations(src,
                new FileCallableImpl(false, FolderDiff.Entry.Type.NEW));
        assertMarkAsNewOrModified(expected, src, dst);
    }

    @Test
    public void getFiles2Delete_allcopied_AllFound() throws IOException {
        Set<FolderDiff.Entry> expected = createFilesAndExpectations(src, new FileCallable() {
            public void process(File file, Set<FolderDiff.Entry> expected) throws IOException {
                if (file.delete()) {
                    String relativeName = FolderDiff.getRelativeName(file.getAbsolutePath(), src.getAbsolutePath());
                    expected.add(new FolderDiff.Entry(relativeName, FolderDiff.Entry.Type.DELETED));
                }
            }
        });
        assertMarkAsDelete(expected, src, dst);
    }

    // the file is newly created in dst, we shouldn't delete this file
    // therefore, we don't need to add to expected
    @Test
    public void getFiles2Delete_createNewFile_MarkAsDelete() throws IOException {
        Set<FolderDiff.Entry> expected = createFilesAndExpectations(dst,
                new FileCallableImpl(false, FolderDiff.Entry.Type.DELETED));
        assertMarkAsDelete(expected, src, dst);
    }

    // the file is copied in dst, the last modified time should be same as the
    // original one
    // we should delete this file
    @Test
    public void getFiles2Delete_CopiedFilesInDst_AllFound() throws IOException {
        Set<FolderDiff.Entry> expected = createFilesAndExpectations(dst,
                new FileCallableImpl(true, FolderDiff.Entry.Type.DELETED));
        assertMarkAsDelete(expected, src, dst);
    }

    @Test
    public void getFiles2Delete_CopiedFilesInDstWithAllowDeleteList_AllFound() throws IOException {
        Collection<File> list = FileUtils.listFiles(dst, null, true);
        Set<String> allowDeleteList = new HashSet<String>();
        for (File file : list) {
            String relativePath = FolderDiff.getRelativeName(file.getAbsolutePath(), dst.getAbsolutePath());
            allowDeleteList.add(relativePath);
        }

        Set<FolderDiff.Entry> expected = createFilesAndExpectations(dst, new FileCallable() {
            public void process(File file, Set<FolderDiff.Entry> expected) throws IOException {
                // the file is copied in dst, the last modified time should be same as the
                // original one
                // we should delete this file
                File newFile = createNewFile(file, true);
            }
        });
        FolderDiff diff = getFolderDiff(src, dst);
        diff.setAllowDeleteList(allowDeleteList);
        List<FolderDiff.Entry> result = diff.getFiles2Delete(false);
        assertEquals(expected, new HashSet<FolderDiff.Entry>(result));
    }

    private void assertMarkAsNewOrModified(Set<FolderDiff.Entry> expected, File src, File dst) {
        FolderDiff diff = getFolderDiff(src, dst);
        List<FolderDiff.Entry> result = diff.getNewOrModifiedFiles(checkTime, false);
        assertEquals(expected, new HashSet<FolderDiff.Entry>(result));
    }

    private void assertMarkAsDelete(Set<FolderDiff.Entry> expected, File src, File dst) {
        FolderDiff diff = getFolderDiff(src, dst);
        List<FolderDiff.Entry> result = diff.getFiles2Delete(false);
        assertEquals(expected, new HashSet<FolderDiff.Entry>(result));
    }

    private File createNewFile(File srcFile, boolean copyFile) throws IOException {
        String ext = FilenameUtils.getExtension(srcFile.getAbsolutePath());
        if (null != ext && ext.length() > 0)
            ext = "." + ext;
        String base = FilenameUtils.removeExtension(srcFile.getAbsolutePath());
        for (int i = 0;; i++) {
            String newName = null;
            if (0 == i)
                newName = base + "New" + ext;
            else
                newName = base + "New" + i + ext;
            File newFile = new File(newName);
            if (!newFile.exists()) {
                if (copyFile) {
                    FileUtils.copyFile(srcFile, newFile);
                    // newFile.setLastModified(System.currentTimeMillis());
                } else {
                    PrintWriter writer = new PrintWriter(newFile);
                    writer.println("This is a FolderDiff Test File");
                    writer.flush();
                    writer.close();
                }
                return newFile;
            }
        }
    }

    private FolderDiff getFolderDiff(File src, File dst) {
        return new FolderDiffFake(src.getAbsolutePath(), dst.getAbsolutePath());
    }

    private Set<FolderDiff.Entry> createFilesAndExpectations(File folder, FileCallable call) throws IOException {
        ArrayList<File> files = new ArrayList<File>(FileUtils.listFiles(folder, null, true));
        Collections.shuffle(files);
        Set<FolderDiff.Entry> expected = new HashSet<FolderDiff.Entry>();
        for (int i = 0; i < Math.min(10, files.size() / 2); i++) {
            File file = (File) files.get(i);
            call.process(file, expected);
        }
        return expected;
    }

    private interface FileCallable {
        public void process(File file, Set<FolderDiff.Entry> expected) throws IOException;
    }

    private class FileCallableImpl implements FileCallable {

        boolean copyFile;
        FolderDiff.Entry.Type type;

        public FileCallableImpl(boolean copyFile, FolderDiff.Entry.Type type) {
            this.copyFile = copyFile;
            this.type = type;
        }

        @Override
        public void process(File file, Set<Entry> expected) throws IOException {
            File newFile = createNewFile(file, copyFile);
            String relativeName;
            if (type == FolderDiff.Entry.Type.DELETED) {
                relativeName = FolderDiff.getRelativeName(newFile.getAbsolutePath(), dst.getAbsolutePath());
            } else {
                relativeName = FolderDiff.getRelativeName(newFile.getAbsolutePath(), src.getAbsolutePath());
            }
            expected.add(new FolderDiff.Entry(relativeName, type));
        }
    }

    @Test
    public void getFiles2Delete_noRemovedSrcFiles_nothing2Delete() throws IOException {
        assertMarkAsDelete(new HashSet<FolderDiff.Entry>(), src1, dst1);
    }

    @Test
    public void getFiles2Delete_allDestinationFolderDeleted_nothing2Delete() throws IOException {
        FileUtils.deleteDirectory(dst1);
        assertMarkAsDelete(new HashSet<FolderDiff.Entry>(), src1, dst1);
    }

    @Test
    public void getFiles2Delete_aSourceFolderDeleted_markAllFilesForDeletion() throws IOException {
        FileUtils.deleteDirectory(src1);
        Set<FolderDiff.Entry> expected = new HashSet<FolderDiff.Entry>();
        expected.add(new Entry(folderFilePath, FolderDiff.Entry.Type.DELETED));
        expected.add(new Entry(rootFilePath, FolderDiff.Entry.Type.DELETED));
        expected.add(new Entry(subfolderFilePath, FolderDiff.Entry.Type.DELETED));
        assertMarkAsDelete(expected, src1, dst1);
    }
}