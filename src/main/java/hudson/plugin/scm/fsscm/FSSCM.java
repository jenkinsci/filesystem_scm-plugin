package hudson.plugin.scm.fsscm;

import java.io.*;
import java.util.*;
import org.kohsuke.stapler.StaplerRequest;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import org.apache.commons.lang.StringUtils;
import java.text.SimpleDateFormat;

public class FSSCM extends SCM {

	private String path;
	private boolean clearWorkspace;
	private boolean filterEnabled;
	private boolean includeFilter;
	private String[] filters;
	
	// Don't use DataBoundConsturctor, it is still not mature enough, many HTML form elements are not binded
	// @DataBoundConstructor
    public FSSCM(String path, boolean clearWorkspace, boolean filterEnabled, boolean includeFilter, String[] filters) {
    	this.path = path;
    	this.clearWorkspace = clearWorkspace;
    	this.filterEnabled = filterEnabled;
    	this.includeFilter = includeFilter;
   		if ( null != filters ) {
   			Vector<String> v = new Vector<String>();
   			for(int i=0; i<filters.length; i++) {
   				// remove empty strings
   				if ( StringUtils.isNotEmpty(filters[i]) ) {
   					v.add(filters[i]);
   				}
   			}
   			this.filters = (String[]) v.toArray(new String[1]); 
   		} else {
   			this.filters = null;
   		}
    }
    
	public String getPath() {
		return path;
	}

	public String[] getFilters() {
		return filters;
	}
	
	public boolean isFilterEnabled() {
		return filterEnabled;
	}
	
	public boolean isIncludeFilter() {
		return includeFilter;
	}
	
	public boolean isClearWorkspace() {
		return clearWorkspace;
	}
	
	@Override
	public boolean checkout(AbstractBuild build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) 
	throws IOException, InterruptedException {
		
		long start = System.currentTimeMillis();
		PrintStream log = launcher.getListener().getLogger();
		log.println("FSSCM.checkout " + path + " to " + workspace);
		Boolean b = Boolean.TRUE;
		
		if ( clearWorkspace ) {
			log.println("FSSCM.clearWorkspace...");
			workspace.deleteRecursive();
			b = workspace.act(new RemoteCopyDir(path));
			
		// not clearWorkspace
		} else {
			RemoteFolderDiff.CheckOut callable = new RemoteFolderDiff.CheckOut();
			setupRemoteFolderDiff(callable, build.getProject());
			List<FolderDiff.Entry> list = workspace.act(callable);
			
			// raw log
			String str = callable.getLog();
			if ( str.length() > 0 ) log.println(str);
			
			ChangelogSet.XMLSerializer handler = new ChangelogSet.XMLSerializer();
			ChangelogSet changeLogSet = new ChangelogSet(build, list);
			handler.save(changeLogSet, changelogFile);
		}
		
		log.println("FSSCM.check completed in " + formatDurration(System.currentTimeMillis()-start));
		return b;
	}
	
	@Override
	public ChangeLogParser createChangeLogParser() {
		return new ChangelogSet.XMLSerializer();
	}

	@Override
	public SCMDescriptor<FSSCM> getDescriptor() {
		return DescriptorImpl.DESCRIPTOR;
	}

	/**
	 * There are two things we need to check
	 * <ul>
	 *   <li>files created or modified since last build time, we only need to check the source folder</li>
	 *   <li>file deleted since last build time, we have to compare source and destination folder</li>
	 * </ul>
	 */
	@Override
	public boolean pollChanges(AbstractProject project, Launcher launcher, FilePath workspace, TaskListener listener) 
	throws IOException, InterruptedException {
		
		long start = System.currentTimeMillis();
		
		PrintStream log = launcher.getListener().getLogger();
		log.println("FSSCM.pollChange: " + path);
		
		RemoteFolderDiff.PollChange callable = new RemoteFolderDiff.PollChange();
		setupRemoteFolderDiff(callable, project);
		
		boolean changed = workspace.act(callable);
		String str = callable.getLog();
		if ( str.length() > 0 ) log.println(str);
		log.println("FSSCM.pollChange return " + changed);

		log.println("FSSCM.poolChange completed in " + formatDurration(System.currentTimeMillis()-start));		
		return changed;
	}
	
	private void setupRemoteFolderDiff(RemoteFolderDiff diff, AbstractProject project) {
		Run lastBuild = project.getLastBuild();
		if ( null == lastBuild ) {
			diff.setLastBuildTime(0);
			diff.setLastSuccessfulBuildTime(0);
		} else {
			diff.setLastBuildTime(lastBuild.getTimestamp().getTimeInMillis());
			Run lastSuccessfulBuild = project.getLastSuccessfulBuild();
			if ( null == lastSuccessfulBuild ) {
				diff.setLastSuccessfulBuildTime(-1);
			} else {
				diff.setLastSuccessfulBuildTime(lastSuccessfulBuild.getTimestamp().getTimeInMillis());
			}
		}
		
		diff.setSrcPath(path);
		
		if ( filterEnabled ) {
			if ( includeFilter ) diff.setIncludeFilter(filters);
			else diff.setExcludeFilter(filters);
		}		
	}
		
	protected static String formatDurration(long diff) {
		if ( diff < 60*1000L ) {
			// less than 1 minute
			if ( diff <= 1 ) return diff + " millisecond";
			else if ( diff < 1000L ) return diff + " milliseconds";
			else if ( diff < 2000L ) return ((double)diff/1000.0) + " second";
			else return ((double)diff/1000.0) + " seconds";
		} else {
			return org.apache.commons.lang.time.DurationFormatUtils.formatDurationWords(diff, true, true);
		}
	}
	
    public static final class DescriptorImpl extends SCMDescriptor<FSSCM>
    {
        public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();
            	
        private DescriptorImpl() {
        	super(FSSCM.class, null);
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "File System";
        }

        @Override
        public boolean configure(StaplerRequest req) throws FormException {
            return true;
        }        
        
        @Override
        public SCM newInstance(StaplerRequest req) throws hudson.model.Descriptor.FormException {
        	String path = req.getParameter("fs_scm.path");
        	String[] filters = req.getParameterValues("fs_scm.filters");
        	Boolean filterEnabled = Boolean.valueOf("on".equalsIgnoreCase(req.getParameter("fs_scm.filterEnabled")));
        	Boolean includeFilter = Boolean.valueOf(req.getParameter("fs_scm.includeFilter"));
        	Boolean clearWorkspace = Boolean.valueOf("on".equalsIgnoreCase(req.getParameter("fs_scm.clearWorkspace")));
            return new FSSCM(path, clearWorkspace, filterEnabled, includeFilter, filters);
        }
        
    }		
}
