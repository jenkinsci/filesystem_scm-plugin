package hudson.plugins.filesystem_scm;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import hudson.remoting.VirtualChannel;
import jenkins.SlaveToMasterFileCallable;

public class RemoteCopyDir extends SlaveToMasterFileCallable<Boolean> {

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
