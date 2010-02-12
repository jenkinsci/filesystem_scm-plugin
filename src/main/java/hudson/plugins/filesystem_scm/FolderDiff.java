package hudson.plugins.filesystem_scm;

import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.*;

/** Detect if two folders are the same or not
 * 
 * <p>This is the core logic for detecting if we need to checkout or pollchanges</p>
 * 
 * <p>Two methods to detect if the two folders are the same
 * <ul>
 *   <li>check if there are new/modified files in the source folder</li>
 *   <li>check if there are deleted files in the source folder</li>
 * </ul>
 * </p>
 * 
 * @author Sam NG
 *
 */
public class FolderDiff implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String srcPath;
	private String dstPath;
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
	
	public void setIncludeFilter(String[] filters) {
		filterEnabled = true;
		includeFilter = true;
		this.filters = filters;
	}
	
	public void setExcludeFilter(String[] filters) {
		filterEnabled = true;
		includeFilter = false;
		this.filters = filters;		
	}
	
	public void setAllowDeleteList(Set<String> allowDeleteList) {
		this.allowDeleteList = allowDeleteList;
	}
	
	/*
	public boolean isModifiedSince(long time) {
		//if ( hasNewOrModifiedFiles(time) ) return true;
		//else if ( hasDeletedFiles(time) ) return true;
		//else return false;
		return true;
	}*/
	
	/**
	 * <p>For each file in the source folder
	 * <ul>
	 *   <li>if file is not in destination, this is a new file</li>
	 *   <li>if the destination file exists but is old, this is a modified file</li>
	 * </ul>
	 * 
	 * <p>Note: the time parameter (1st param) is basically not used in the code. 
	 * On Windows, the lastModifiedDate will not be updated when you copy a file to the source folder, 
	 * until we have a way to get the "real" lastModifiedDate on Windows, we won't use this "time" field</p>
	 * 
	 * @param time should be the last build time, to improve performance, we will list all files modified after "time" and check with destination
	 * @param breakOnceFound to improve performance, we will return once we found the 1st new or modified file
	 * @param testRun if true, will not sync file from source to destination, otherwise, will sync files if new or modified files found
	 * 
	 * @return the list of new or modified files
	 */
	public List<Entry> getNewOrModifiedFiles(long time, boolean breakOnceFound, boolean testRun) {
		File src = new File(srcPath);
		File dst = new File(dstPath);
		
		IOFileFilter dirFilter = HiddenFileFilter.VISIBLE;
		AndFileFilter fileFilter = new AndFileFilter();
		// AgeFileFilter is base on lastModifiedDate, but if you copy a file on Windows, the lastModifiedDate is not changed
		// only the creation date is updated, so we can't use the following AgeFileFiilter
		// fileFilter.addFileFilter(new AgeFileFilter(time, false /* accept newer */));
		fileFilter.addFileFilter(HiddenFileFilter.VISIBLE);
		if ( filterEnabled && null != filters && filters.length > 0 ) {
			WildcardFileFilter wcf = new WildcardFileFilter(filters, IOCase.INSENSITIVE);
			if ( includeFilter ) {
				fileFilter.addFileFilter(wcf);
			} else {
				fileFilter.addFileFilter(new NotFileFilter(wcf));				
			}
		}
		Iterator<File> it = (Iterator<File>)FileUtils.iterateFiles(src, fileFilter, dirFilter);
		ArrayList<Entry> list = new ArrayList<Entry>();
		while( it.hasNext() ) {
			File file = it.next();
			try {
				String relativeName = getRelativeName(file.getAbsolutePath(), src.getAbsolutePath());
				boolean newOrModified = false;
				// need to change dst to see if there is such a file
				File tmp = new File(dst, relativeName);
				if ( !tmp.exists() ) {
					newOrModified = true;
					list.add(new Entry(relativeName, Entry.Type.NEW));
					log("New file: " + relativeName);
				} else if ( FileUtils.isFileNewer(file, time) || FileUtils.isFileNewer(file, tmp) ) {
					newOrModified = true;
					list.add(new Entry(relativeName, Entry.Type.MODIFIED));
					log("Modified file: " + relativeName);
				}
				if ( newOrModified ) {
					if ( breakOnceFound ) return list;
					if ( !testRun ) {
						// FileUtils.copyFile(file, tmp);
						copyFile(file, tmp);
					}					
				}
			} catch ( IOException e ) {
				log(e);
			}
		}
		return list;
	}
	
	/**
	 * <p>For each file in the destination folder
	 * <ul>
	 *   <li>if file is not in source, and it is in the allowDeleteList, this file will be deleted in the source</li>
	 * </ul>
	 * 
	 * <p>Note: the time parameter (1st param) is basically not used in the code. 
	 * On Windows, the lastModifiedDate will not be updated when you copy a file to the source folder, 
	 * until we have a way to get the "real" lastModifiedDate on Windows, we won't use this "time" field</p>
	 * 
	 * @param time should be the last build time, to improve performance, we will list all files modified after "time" and check with source
	 * @param breakOnceFound to improve performance, we will return once we found the 1st new or modified file
	 * @param testRun if true, will not sync file from source to destination, otherwise, will sync files if deleted files found
	 * 
	 * @return the list of deleted files
	 */	
	public List<Entry> getDeletedFiles(long time, boolean breakOnceFound, boolean testRun) {
		File src = new File(srcPath);
		File dst = new File(dstPath);
		
		IOFileFilter dirFilter = HiddenFileFilter.VISIBLE;
		AndFileFilter fileFilter = new AndFileFilter();
		// AgeFileFilter is base on lastModifiedDate, but if you copy a file on Windows, the lastModifiedDate is not changed
		// only the creation date is updated, so we can't use the following AgeFileFiilter
		//fileFilter.addFileFilter(new AgeFileFilter(time, true /* accept older */));
		fileFilter.addFileFilter(HiddenFileFilter.VISIBLE);
		if ( filterEnabled && null != filters && filters.length > 0 ) {
			WildcardFileFilter wcf = new WildcardFileFilter(filters, IOCase.INSENSITIVE);
			if ( includeFilter ) {
				fileFilter.addFileFilter(wcf);
			} else {
				fileFilter.addFileFilter(new NotFileFilter(wcf));				
			}
		}
		Iterator<File> it = (Iterator<File>)FileUtils.iterateFiles(dst, fileFilter, dirFilter);
		ArrayList<Entry> list = new ArrayList<Entry>();
		while(it.hasNext()) {
			File file  = it.next();
			try {
				String relativeName = getRelativeName(file.getAbsolutePath(), dst.getAbsolutePath());
				File tmp = new File(src, relativeName);
				if ( !tmp.exists() && (null == allowDeleteList || allowDeleteList.contains(relativeName)) ) {
					log("Deleted file: " + relativeName);
					list.add(new Entry(relativeName, Entry.Type.DELETED));
					if ( breakOnceFound ) return list;
					if ( !testRun ) {
						try {
							boolean deleted = file.delete();
							if ( !deleted ) {
								log("file.delete() failed: " + file.getAbsolutePath());
							}
						} catch ( SecurityException e ) {
							log("Can't delete " + file.getAbsolutePath(), e);
						}
					}
				}
			} catch ( IOException e ) {
				log(e);
			}			
		}
		return list;
	}	
	
	/** This function will convert e.stackTrace to String and call log(String) 
	 * 
	 * @param msg
	 * @param e
	 */
	protected void log(Exception e ) {
		log(stackTraceToString(e));
	}
	
	/** This function will convert e.stackTrace to String and call log(String) 
	 * 
	 * @param msg
	 * @param e
	 */
	protected void log(String msg, Exception e) {
		log(msg + "\n" + stackTraceToString(e));
	}
	
	/** Default log to System.out
	 * 
	 * @param msg
	 */
	protected void log(String msg) {
		System.out.println(msg);
	}
	
	/** Convert Exception.stackTrace to String
	 * 
	 * @param e
	 * @return
	 */
	public static String stackTraceToString(Exception e ) {
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
	 *   <li>fileName = c:\abc\def\foo.java</li>
	 *   <li>folderName = c:\abc</li>
	 *   <li>relativeName = def\foo.java
	 * </ul> 
	 * This function will not handle Unix/Windows path separator conversation, but will append a java.io.File.separator if folderName does not end with one
	 * @param fileName the full path of the file, usually file.getAbsolutePath()
	 * @param folderName the full path of the folder, usually dir.getAbsolutePath()
	 * @return the relativeName of fileNamae and folderName
	 * @throws IOException if fileName is not relative to folderName
	 */
	public static String getRelativeName(String fileName, String folderName) throws IOException {
		// make sure there is an end separator after folderName
		String sep = java.io.File.separator;
		if ( !folderName.endsWith(sep) ) folderName += sep;
		int x = fileName.indexOf(folderName);
		if ( 0 != x ) throw new IOException(fileName + " is not inside " + folderName);
		String relativeName = fileName.substring(folderName.length() );
		return relativeName;
	}
	
	/** Copy file from source to destination (default will not copy file permission)
	 * 
	 * @param src Source File
	 * @param dst Destination File
	 * @throws IOException
	 */
	protected void copyFile(File src, File dst) throws IOException {
		FileUtils.copyFile(src, dst);
	}
	
	public static class Entry implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		private String filename;
		private Type type;
		
		public enum Type { MODIFIED, NEW, DELETED };
		
		public Entry() { }
		
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
			result = prime * result
					+ ((filename == null) ? 0 : filename.hashCode());
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
