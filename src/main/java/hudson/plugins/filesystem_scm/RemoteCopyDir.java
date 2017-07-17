package hudson.plugins.filesystem_scm;

import hudson.RestrictedSince;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import hudson.remoting.VirtualChannel;
import jenkins.MasterToSlaveFileCallable;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * @deprecated Not used anymore
 */
@Deprecated
@Restricted(NoExternalUse.class)
@RestrictedSince("1.21")
public class RemoteCopyDir extends MasterToSlaveFileCallable<Boolean> {

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
