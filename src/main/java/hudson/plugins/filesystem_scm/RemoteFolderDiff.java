package hudson.plugins.filesystem_scm;

import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;
import hudson.tools.JDKInstaller.Platform;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RemoteFolderDiff extends FolderDiff {

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
		boolean isUnix = false;
		try {
			if (Platform.WINDOWS != Platform.current()) isUnix = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			// if not write-able, then we can't copy, have to set it to write-able
			if ( isUnix && dstpath.exists() ) {
				int mode = dstpath.mode();
				// owner write-able bit = 010 000 000b = 0x80 
				if ( (mode & 0x80) == 0 ) {
					dstpath.chmod(mode | 0x80);
				}
			}
			srcpath.copyToWithPermission(dstpath);
		} catch (InterruptedException e) {
			IOException ioe = new IOException();
			ioe.initCause(e);
			throw ioe;
		}
	}
	
	public String getLog() {
		return buf.toString();
	}
	
	public static class PollChange extends RemoteFolderDiff implements FileCallable<Boolean> {
		
		private static final long serialVersionUID = 1L; 
		
		public Boolean invoke(File workspace, VirtualChannel channel) throws IOException {
			setDstPath(workspace.getAbsolutePath());
			List<FolderDiff.Entry> newFiles = getNewOrModifiedFiles(lastBuildTime, true, true);
			if ( newFiles.size() > 0 ) return Boolean.TRUE;
			if ( -1 == lastSuccessfulBuildTime ) return Boolean.FALSE;
			List<FolderDiff.Entry> delFiles = getDeletedFiles(lastSuccessfulBuildTime, true, true);
			return delFiles.size() > 0;
		}		
	}
	
	public static class CheckOut extends RemoteFolderDiff implements FileCallable< List<FolderDiff.Entry> > {

		private static final long serialVersionUID = 1L; 

		public List<FolderDiff.Entry> invoke(File workspace, VirtualChannel channel) throws IOException {
			setDstPath(workspace.getAbsolutePath());
			List<FolderDiff.Entry> newFiles = getNewOrModifiedFiles(lastBuildTime, false, false);
			List<FolderDiff.Entry> delFiles = getDeletedFiles(lastSuccessfulBuildTime, false, false);
			List<FolderDiff.Entry> files = new ArrayList<FolderDiff.Entry>();
			files.addAll(newFiles);
			files.addAll(delFiles);
			return files;
		}	
	}
}
