package hudson.plugins.filesystem_scm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;

public class FolderDiffFake<T> extends FolderDiff<T> {

    List<ImmutablePair<File, File>> copyFilePairs;
    List<File> deleteFiles;

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
    protected void copyFile(File src, File dst) throws IOException {
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
