package hudson.plugins.filesystem_scm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class FolderDiffFake<T> extends FolderDiff<T> {

    List<Pair<File, File>> filePairs;

    public FolderDiffFake() {
        filePairs = new ArrayList();
    }

    /**
     * Overriden for Testing purposes only log the files which should have been
     * copied
     */
    @Override
    protected void copyFile(File src, File dst) throws IOException {
        filePairs.add(new ImmutablePair(src, dst));
    }
}
