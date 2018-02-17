package hudson.plugins.filesystem_scm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class FolderDiffFake<T> extends FolderDiff<T> {

    List<ImmutablePair<File, File>> copyFilePairs;
    List<File> deleteFiles;

    public FolderDiffFake() {
        copyFilePairs = new ArrayList<ImmutablePair<File, File>>();
        deleteFiles = new ArrayList<File>();
    }

    /**
     * Overriden for Testing purposes only log the files which should have been
     * copied
     */
    @Override
    protected void copyFile(File src, File dst) throws IOException {
        copyFilePairs.add(new ImmutablePair<File, File>(src, dst));
    }

    /**
     * Overriden for Testing purposes only log the files which should have been
     * deleted
     */
    @Override
    protected boolean deleteFile(File file) {
        deleteFiles.add(file);
        return true;
    }
}
