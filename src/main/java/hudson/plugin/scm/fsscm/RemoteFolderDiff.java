package hudson.plugin.scm.fsscm;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

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
