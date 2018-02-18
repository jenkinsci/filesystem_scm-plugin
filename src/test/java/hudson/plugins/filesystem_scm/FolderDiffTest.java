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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FolderDiffTest {

    File src;
    File dst;
    @Rule
    public TemporaryFolder srcFolder = new TemporaryFolder();
    @Rule
    public TemporaryFolder dstFolder = new TemporaryFolder();
    long modifiedTime;
    long checkTime;
    final static long diff = 60 * 60 * 2 * 1000L;

    @Before
    public void setupSrcAndDst() throws IOException {
        src = srcFolder.getRoot();
        for (int i = 0; i < 50; i++)
            createRandomFile(src);
        dst = dstFolder.getRoot();
        FileUtils.copyDirectory(src, dst);

        checkTime = System.currentTimeMillis() - 60 * 60 * 1000L;
        modifiedTime = System.currentTimeMillis() - diff;
    }

    private static File createRandomFile(File dir) {
        while (true) {
            String name = getRandomName();
            File tmp = new File(dir, name);
            if (!tmp.exists()) {
                try {
                    FileUtils.touch(tmp);
                    tmp.setLastModified(System.currentTimeMillis() - 2 * diff);
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
    public void testNoChanges() {
        FolderDiff diff = getFolderDiff();
        List<FolderDiff.Entry> result = diff.getNewOrModifiedFiles(checkTime, false);
        assertTrue(result.isEmpty());
        ChangelogSet changelogSet = new ChangelogSet(null, result);
        assertTrue(changelogSet.isEmptySet());
    }

    @Test
    public void testModifiedFiles() throws IOException {
        Set<FolderDiff.Entry> expected = processFiles(src, new FileCallable() {
            public void process(File file, Set<FolderDiff.Entry> expected) throws IOException {
                boolean modified = file.setLastModified(modifiedTime);
                if (!modified) {
                    throw new IOException("setlastModified failed");
                }
                String relativeName = FolderDiff.getRelativeName(file.getAbsolutePath(), src.getAbsolutePath());
                expected.add(new FolderDiff.Entry(relativeName, FolderDiff.Entry.Type.MODIFIED));
            }
        });
        assertNewOrModified(expected);
    }

    @Test
    public void testCopiedFiles() throws IOException {
        Set<FolderDiff.Entry> expected = processFiles(src, new FileCallable() {
            public void process(File file, Set<FolderDiff.Entry> expected) throws IOException {
                createFileAndAddExpectation(file, expected, true, FolderDiff.Entry.Type.NEW);
            }
        });
        assertNewOrModified(expected);
    }

    @Test
    public void testCreatedFiles() throws IOException {
        Set<FolderDiff.Entry> expected = processFiles(src, new FileCallable() {
            public void process(File file, Set<FolderDiff.Entry> expected) throws IOException {
                createFileAndAddExpectation(file, expected, false, FolderDiff.Entry.Type.NEW);
            }
        });
        assertNewOrModified(expected);
    }

    @Test
    public void testDeletedFiles() throws IOException {
        Set<FolderDiff.Entry> expected = processFiles(src, new FileCallable() {
            public void process(File file, Set<FolderDiff.Entry> expected) throws IOException {
                if (file.delete()) {
                    String relativeName = FolderDiff.getRelativeName(file.getAbsolutePath(), src.getAbsolutePath());
                    expected.add(new FolderDiff.Entry(relativeName, FolderDiff.Entry.Type.DELETED));
                }
            }
        });
        assertDeleted(expected);
    }

    @Test
    public void testNewFilesInDst() throws IOException {
        Set<FolderDiff.Entry> expected = processFiles(dst, new FileCallable() {
            public void process(File file, Set<FolderDiff.Entry> expected) throws IOException {
                // the file is newly created in dst, we shouldn't delete this file
                // therefore, we don't need to add to expected
                createFileAndAddExpectation(file, expected, false, FolderDiff.Entry.Type.DELETED);
            }
        });
        assertDeleted(expected);
    }

    @Test
    public void testCopiedFilesInDst() throws IOException {
        Set<FolderDiff.Entry> expected = processFiles(dst, new FileCallable() {
            public void process(File file, Set<FolderDiff.Entry> expected) throws IOException {
                // the file is copied in dst, the last modified time should be same as the
                // original one
                // we should delete this file
                createFileAndAddExpectation(file, expected, true, FolderDiff.Entry.Type.DELETED);
            }

        });
        assertDeleted(expected);
    }

    @Test
    public void testCopiedFilesInDstWithAllowDeleteList() throws IOException {
        Collection<File> list = FileUtils.listFiles(dst, null, true);
        Set<String> allowDeleteList = new HashSet<String>();
        for (File file : list) {
            String relativePath = FolderDiff.getRelativeName(file.getAbsolutePath(), dst.getAbsolutePath());
            allowDeleteList.add(relativePath);
        }

        Set<FolderDiff.Entry> expected = processFiles(dst, new FileCallable() {
            public void process(File file, Set<FolderDiff.Entry> expected) throws IOException {
                // the file is copied in dst, the last modified time should be same as the
                // original one
                // we should delete this file
                File newFile = createNewFile(file, true);
            }
        });
        FolderDiff diff = getFolderDiff();
        diff.setAllowDeleteList(allowDeleteList);
        List<FolderDiff.Entry> result = diff.getDeletedFiles(checkTime, false);
        assertEquals(expected, new HashSet<FolderDiff.Entry>(result));
    }

    private void createFileAndAddExpectation(File file, Set<FolderDiff.Entry> expected, boolean copyFile,
            FolderDiff.Entry.Type type) throws IOException {
        File newFile = createNewFile(file, copyFile);
        String relativeName;
        if (type == FolderDiff.Entry.Type.DELETED) {
            relativeName = FolderDiff.getRelativeName(newFile.getAbsolutePath(), dst.getAbsolutePath());
        } else {
            relativeName = FolderDiff.getRelativeName(newFile.getAbsolutePath(), src.getAbsolutePath());
        }
        expected.add(new FolderDiff.Entry(relativeName, type));
    }

    private void assertNewOrModified(Set<FolderDiff.Entry> expected) {
        FolderDiff diff = getFolderDiff();
        List<FolderDiff.Entry> result = diff.getNewOrModifiedFiles(checkTime, false);
        assertEquals(expected, new HashSet<FolderDiff.Entry>(result));
    }

    private void assertDeleted(Set<FolderDiff.Entry> expected) {
        FolderDiff diff = getFolderDiff();
        List<FolderDiff.Entry> result = diff.getDeletedFiles(checkTime, false);
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

    private FolderDiff getFolderDiff() {
        FolderDiff folderDiff = new FolderDiffFake();
        folderDiff.setSrcPath(src.getAbsolutePath());
        folderDiff.setDstPath(dst.getAbsolutePath());
        return folderDiff;
    }

    private Set<FolderDiff.Entry> processFiles(File folder, FileCallable call) throws IOException {
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
        public void process(File file, Set<FolderDiff.Entry> result) throws IOException;
    }
}
