package hudson.plugins.filesystem_scm;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AndFileFilter;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.plugins.filesystem_scm.FolderDiff.Entry.Type;
import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;

/**
 * Detect if two folders are the same or not
 * 
 * <p>
 * This is the core logic for detecting if we need to checkout or pollchanges
 * </p>
 * 
 * <p>
 * Two methods to detect if the two folders are the same
 * </p>
 * <ul>
 * <li>check if there are new/modified files in the source folder</li>
 * <li>check if there are deleted files in the source folder</li>
 * </ul>
 * 
 * @param <T>
 *            Type of the item being returned by the callable
 * @author Sam NG
 *
 */
public class FolderDiff<T> extends MasterToSlaveFileCallable<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private String srcPath;
    private String dstPath;
    private boolean ignoreHidden;
    private boolean filterEnabled;
    private boolean includeFilter;
    private String[] filters;
    private Set<String> allowDeleteList;

    public FolderDiff() {
        filterEnabled = false;
    }

    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }

    public void setDstPath(String dstPath) {
        this.dstPath = dstPath;
    }

    public void setIgnoreHidden(boolean ignoreHidden) {
        this.ignoreHidden = ignoreHidden;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Handled on the Changelog class level")
    public void setIncludeFilter(String[] filters) {
        filterEnabled = true;
        includeFilter = true;
        this.filters = filters;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Handled on the Changelog class level")
    public void setExcludeFilter(String[] filters) {
        filterEnabled = true;
        includeFilter = false;
        this.filters = filters;
    }

    public void setAllowDeleteList(Set<String> allowDeleteList) {
        this.allowDeleteList = allowDeleteList;
    }

    /**
     * 
     * @param time
     *            should be the last build time, to improve performance, we will
     *            list all files modified after "time" and check with destination
     * @param breakOnceFound
     *            to improve performance, we will return once we found the 1st new
     *            or modified file
     * @param testRun
     *            is ignored
     * @return the list of new or modified files
     * @deprecated use the method without testrun, if you just want to have some
     *             test mode, inherit this class and overwrite the copy method
     */
    @Deprecated
    public List<Entry> getNewOrModifiedFiles(long time, boolean breakOnceFound, boolean testRun) {
        List<Entry> entries = new ArrayList();
        try {
            entries = getNewOrModifiedFiles(time, breakOnceFound);
        } catch (IOException e) {
            log(e);
        }
        return entries;
    }

    /**
     * <p>
     * For each file in the source folder
     * <ul>
     * <li>if file is not in destination, this is a new file</li>
     * <li>if the destination file exists but is old, this is a modified file</li>
     * </ul>
     * 
     * <p>
     * Note: the time parameter (1st param) is basically not used in the code. On
     * Windows, the lastModifiedDate will not be updated when you copy a file to the
     * source folder, until we have a way to get the "real" lastModifiedDate on
     * Windows, we won't use this "time" field
     * </p>
     * 
     * @param time
     *            should be the last build time, to improve performance, we will
     *            list all files modified after "time" and check with destination
     * @param breakOnceFound
     *            to improve performance, we will return once we found the 1st new
     *            or modified file
     * 
     * @return the list of new or modified files
     * @throws IOException
     *             when source directory is not found or copying is not successful
     */
    public List<Entry> getNewOrModifiedFiles(long time, boolean breakOnceFound) throws IOException {
        File src = new File(srcPath);
        File dst = new File(dstPath);

        ArrayList<Entry> list = new ArrayList<Entry>();
        if (src.isDirectory()) {
            Iterator<File> it = (Iterator<File>) FileUtils.iterateFiles(src, createAntPatternFileFilter(),
                    getDirFilter());
            while (it.hasNext()) {
                File file = it.next();
                String relativeName = getRelativeName(file.getAbsolutePath(), src.getAbsolutePath());
                // need to change dst to see if there is such a file
                File tmp = new File(dst, relativeName);
                boolean newOrModified = true;
                if (!tmp.exists()) {// new
                    list.add(createAndLogg(relativeName, Entry.Type.NEW));
                } else if (FileUtils.isFileNewer(file, time) || FileUtils.isFileNewer(file, tmp)) { // modified
                    list.add(createAndLogg(relativeName, Entry.Type.MODIFIED));
                } else {
                    newOrModified = false;
                }
                if (newOrModified) {
                    if (breakOnceFound) {
                        return list;
                    }
                    copyFile(file, tmp);
                }
            }
        } else {
            throw new IOException(String.format("Source Directory not found! (%s)", src.getAbsolutePath()));
        }
        return list;
    }

    private Entry createAndLogg(String relativeName, Type type) {
        log(type.name() + " file: " + relativeName);
        return new Entry(relativeName, type);
    }

    private AndFileFilter createAntPatternFileFilter() {
        AndFileFilter fileFilter = new AndFileFilter();
        fileFilter.addFileFilter(getDirFilter());
        // AgeFileFilter is base on lastModifiedDate, but if you copy a file on Windows,
        // the lastModifiedDate is not changed
        // only the creation date is updated, so we can't use the following
        // AgeFileFiilter
        // fileFilter.addFileFilter(new AgeFileFilter(time, false /* accept newer */));
        if (filterEnabled && null != filters && filters.length > 0) {
            for (int i = 0; i < filters.length; i++) {
                IOFileFilter iof = new SimpleAntWildcardFilter(filters[i]);
                if (includeFilter) {
                    fileFilter.addFileFilter(iof);
                } else {
                    fileFilter.addFileFilter(new NotFileFilter(iof));
                }
            }
        }
        return fileFilter;
    }

    /**
     * 
     * @param time
     *            not used
     * @param breakOnceFound
     *            to improve performance, we will return once we found the 1st new
     *            or modified file
     * @param testRun
     *            not used
     * @return the list of deleted files
     * @deprecated use getFiles2Delete instead, time never has been used anyway and
     *             testrun is no longer supported, instead inherit from this class
     *             and overwrite deleteFiles() for the testmode feature
     */
    @Deprecated
    public List<Entry> getDeletedFiles(long time, boolean breakOnceFound, boolean testRun) {
        List<Entry> entries = new ArrayList();
        try {
            entries = getFiles2Delete(breakOnceFound);
        } catch (IOException e) {
            log(e);
        }
        return entries;
    }

    /**
     * <p>
     * For each file in the destination folder
     * <ul>
     * <li>if file is not in source, and it is in the allowDeleteList, this file
     * will be deleted in the destination</li>
     * </ul>
     * 
     * @param breakOnceFound
     *            to improve performance, we will return once we found the 1st new
     *            or modified file
     * 
     * @return the list of deleted files
     * @throws IOException
     *             if IO error occurs when deleting a file
     */
    public List<Entry> getFiles2Delete(boolean breakOnceFound) throws IOException {
        File src = new File(srcPath);
        File dst = new File(dstPath);

        IOFileFilter dirFilter = getDirFilter();
        AndFileFilter fileFilter = createAntPatternFileFilter();
        // this is the full list of all viewable/available source files
        Collection<File> allSources = new ArrayList<>();
        if (src.isDirectory()) {
            allSources = (Collection<File>) FileUtils.listFiles(src, fileFilter, dirFilter);
        }

        ArrayList<Entry> list = new ArrayList<Entry>();
        if (dst.isDirectory()) {
            // now get the list of all sources in workspace (destination)
            Iterator<File> it = (Iterator<File>) FileUtils.iterateFiles(dst, TrueFileFilter.TRUE, TrueFileFilter.TRUE);
            while (it.hasNext()) {
                File file = it.next();
                String relativeName = getRelativeName(file.getAbsolutePath(), dst.getAbsolutePath());
                File tmp = new File(src, relativeName);
                if (!allSources.contains(tmp) && (null == allowDeleteList || allowDeleteList.contains(relativeName))) {
                    list.add(createAndLogg(relativeName, Type.DELETED));
                    if (breakOnceFound) {
                        return list;
                    }
                    try {
                        boolean deleted = deleteFile(file);
                        if (!deleted) {
                            log("file.delete() failed: " + file.getAbsolutePath());
                        }
                    } catch (SecurityException e) {
                        log("Can't delete " + file.getAbsolutePath(), e);
                    }
                }
            }
        }
        return list;
    }

    private IOFileFilter getDirFilter() {
        return ignoreHidden ? HiddenFileFilter.VISIBLE : TrueFileFilter.TRUE;
    }

    /**
     * should delete the given file
     * 
     * @param file
     *            the file to delete
     * @return true if successful
     * @throws IOException
     *             if an IOError occurs
     */
    protected boolean deleteFile(File file) throws IOException {
        Path path = Paths.get(file.getAbsolutePath());
        return Files.deleteIfExists(path);
    }

    /**
     * This function will convert e.stackTrace to String and call log(String)
     * 
     * @param e
     *            a thrown Exception which shall be logged
     */
    protected void log(Exception e) {
        log(stackTraceToString(e));
    }

    /**
     * This function will convert e.stackTrace to String and call log(String)
     * 
     * @param msg
     *            some message to be logged
     * @param e
     *            a thrown Exception which shall be logged too
     */
    protected void log(String msg, Exception e) {
        log(msg + "\n" + stackTraceToString(e));
    }

    /**
     * Default log to System.out
     * 
     * @param msg
     *            some message to be logged
     */
    protected void log(String msg) {
        System.out.println(msg);
    }

    /**
     * Convert Exception.stackTrace to String
     * 
     * @param e
     *            an Exception which shall be converted to string
     * 
     * @return the exceptions stacktrace as string
     */
    public static String stackTraceToString(Exception e) {
        StringWriter buf = new StringWriter();
        PrintWriter writer = new PrintWriter(buf);
        e.printStackTrace(writer);
        writer.flush();
        buf.flush();
        return buf.toString();
    }

    /**
     * Get the relative path of fileName and folderName
     * <ul>
     * <li>fileName = c:\abc\def\foo.java</li>
     * <li>folderName = c:\abc</li>
     * <li>relativeName = def\foo.java
     * </ul>
     * This function will not handle Unix/Windows path separator conversation, but
     * will append a java.io.File.separator if folderName does not end with one
     * 
     * @param fileName
     *            the full path of the file, usually file.getAbsolutePath()
     * @param folderName
     *            the full path of the folder, usually dir.getAbsolutePath()
     * @return the relativeName of fileNamae and folderName
     * @throws IOException
     *             if fileName is not relative to folderName
     */
    public static String getRelativeName(String fileName, String folderName) throws IOException {
        // make sure there is an end separator after folderName
        String sep = java.io.File.separator;
        if (!folderName.endsWith(sep))
            folderName += sep;
        int x = fileName.indexOf(folderName);
        if (0 != x)
            throw new IOException(fileName + " is not inside " + folderName);
        String relativeName = fileName.substring(folderName.length());
        return relativeName;
    }

    /**
     * Copy file from source to destination (default will not copy file permission)
     * 
     * @param src
     *            Source File
     * @param dst
     *            Destination File
     * @throws IOException
     *             when copying is not successful an exception could be thrown by
     *             the underlying function
     */
    protected void copyFile(File src, File dst) throws IOException {
        FileUtils.copyFile(src, dst);
        // TODO: adjust file permissions here maybe
    }

    @Override
    public T invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
        // Just a default behavior to retain the compatibility
        throw new IOException("The method has not been overridden. Cannot execute");
    }

    public static class Entry implements Serializable {

        private static final long serialVersionUID = 1L;

        private String filename;
        private Type type;

        public enum Type {
            MODIFIED, NEW, DELETED
        };

        public Entry() {
        }

        public Entry(String filename, Type type) {
            this.filename = filename;
            this.type = type;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((filename == null) ? 0 : filename.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final Entry other = (Entry) obj;
            if (filename == null) {
                if (other.filename != null)
                    return false;
            } else if (!filename.equals(other.filename))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }
    }
}
