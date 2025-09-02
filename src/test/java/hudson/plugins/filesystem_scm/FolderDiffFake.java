package hudson.plugins.filesystem_scm;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FolderDiffFake<T> extends FolderDiff<T> {

    final List<ImmutablePair<File, File>> copyFilePairs;
    final List<File> deleteFiles;

    public FolderDiffFake(String sourcePath, String destinationPath) {
        copyFilePairs = new ArrayList<>();
        deleteFiles = new ArrayList<>();
        this.setDstPath(destinationPath);
        this.setSrcPath(sourcePath);
    }

    /**
     * Overridden for Testing purposes only log the files which should have been
     * copied
     */
    @Override
    protected void copyFile(File src, File dst) {
        copyFilePairs.add(new ImmutablePair<>(src, dst));
    }

    /**
     * Overridden for Testing purposes only log the files which should have been
     * deleted
     */
    @Override
    protected boolean deleteFile(File file) {
        deleteFiles.add(file);
        return true;
    }
}
