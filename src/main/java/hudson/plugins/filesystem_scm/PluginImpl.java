package hudson.plugins.filesystem_scm;

import hudson.Plugin;
import hudson.scm.SCMS;;

/**
 * Entry point of a FSSCM Plugin
 *
 * @author Sam NG
 */
public class PluginImpl extends Plugin {
    public void start() throws Exception {
    	SCMS.SCMS.add(FSSCM.DescriptorImpl.DESCRIPTOR);
    }
}
