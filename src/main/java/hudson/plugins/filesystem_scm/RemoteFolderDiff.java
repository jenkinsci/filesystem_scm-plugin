package hudson.plugins.filesystem_scm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import hudson.FilePath;
import hudson.os.PosixException;
import hudson.remoting.VirtualChannel;
import hudson.tools.JDKInstaller.Platform;

public class RemoteFolderDiff<T> extends FolderDiff<T> {

    protected StringBuffer buf;
    protected long lastBuildTime;
    protected long lastSuccessfulBuildTime;

    public RemoteFolderDiff() {
        buf = new StringBuffer();
    }

    public long getLastBuildTime() {
        return lastBuildTime;
    }

    public void setLastBuildTime(long lastBuildTime) {
        this.lastBuildTime = lastBuildTime;
    }

    public long getLastSuccessfulBuildTime() {
        return lastSuccessfulBuildTime;
    }

    public void setLastSuccessfulBuildTime(long lastSuccessfulBuildTime) {
        this.lastSuccessfulBuildTime = lastSuccessfulBuildTime;
    }

    @Override
    protected void log(String msg) {
        buf.append(msg).append("\n");
    }

    @Override
    protected void copyFile(File src, File dst) throws IOException {
        FilePath srcpath = new FilePath(src);
        FilePath dstpath = new FilePath(dst);
        try {
            changeDestinationPropertiesOnUnixSytem(dstpath, isUnix());
            srcpath.copyToWithPermission(dstpath);
        } catch (InterruptedException e) {
            IOException ioe = new IOException();
            ioe.initCause(e);
            throw ioe;
        }
    }

    private static void changeDestinationPropertiesOnUnixSytem(FilePath dstpath, boolean isUnix)
            throws IOException, InterruptedException, PosixException {
        // if not write-able, then we can't copy, have to set it to write-able
        if (isUnix && dstpath.exists()) {
            int mode = dstpath.mode();
            // owner write-able bit = 010 000 000b = 0x80
            if ((mode & 0x80) == 0) {
                dstpath.chmod(mode | 0x80);
            }
        }
    }

    private static boolean isUnix() {
        boolean isUnix = false;
        try {
            isUnix = (Platform.WINDOWS != Platform.current());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isUnix;
    }

    public String getLog() {
        return buf.toString();
    }

    public static class PollChange extends RemoteFolderDiff<Boolean> {

        private static final long serialVersionUID = 1L;

        @Override
        public Boolean invoke(File workspace, VirtualChannel channel) throws IOException {
            setDstPath(workspace.getAbsolutePath());
            List<FolderDiff.Entry> newFiles = getNewOrModifiedFiles(lastBuildTime, true);
            if (newFiles.size() > 0) {
                return Boolean.TRUE;
            }
            if (-1 == lastSuccessfulBuildTime) {
                return Boolean.FALSE;
            }
            List<FolderDiff.Entry> delFiles = getFiles2Delete(true);
            return delFiles.size() > 0;
        }
    }

    public static class CheckOut extends RemoteFolderDiff<List<FolderDiff.Entry>> {

        private static final long serialVersionUID = 1L;

        @Override
        public List<FolderDiff.Entry> invoke(File workspace, VirtualChannel channel) throws IOException {
            setDstPath(workspace.getAbsolutePath());
            List<FolderDiff.Entry> newFiles = getNewOrModifiedFiles(lastBuildTime, false);
            List<FolderDiff.Entry> delFiles = getFiles2Delete(false);
            List<FolderDiff.Entry> files = new ArrayList<FolderDiff.Entry>();
            files.addAll(newFiles);
            files.addAll(delFiles);
            return files;
        }
    }
}
