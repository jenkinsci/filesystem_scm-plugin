package hudson.plugin.scm.fsscm;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import hudson.FilePath.FileCallable;
import hudson.remoting.VirtualChannel;

public class RemoteCopyDir implements FileCallable<Boolean> {

	private static final long serialVersionUID = 1L; 

	private String sourceDir;
	
	public RemoteCopyDir(String sourceDir) {
		this.sourceDir = sourceDir;
	}
	
	public Boolean invoke(File workspace, VirtualChannel channel) throws IOException {
		FileUtils.copyDirectory(new File(sourceDir), workspace);
		return Boolean.TRUE;
	}

}
