package hudson.plugins.filesystem_scm;

import java.io.*;
import java.util.*;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.scm.SCMRevisionState;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * {@link SCM} implementation which watches a file system folder.
 */
public class FSSCM extends SCM {

	/** The source folder
	 * 
	 */
	private String path;
	/** If true, will delete everything in workspace every time before we checkout
	 * 
	 */
	private boolean clearWorkspace;
	/** If true, will copy hidden files and folders. Default is false.
	 * 
	 */
	private boolean copyHidden;
	/** If we have include/exclude filter, then this is true
	 * 
	 */
	private boolean filterEnabled;
	/** Is this filter a include filter or exclude filter
	 * 
	 */
	private boolean includeFilter;
	/** filters will be passed to org.apache.commons.io.filefilter.WildcardFileFilter
	 * 
	 */
	private String[] filters;
	
	// Don't use DataBoundConsturctor, it is still not mature enough, many HTML form elements are not binded
	// @DataBoundConstructor
    public FSSCM(String path, boolean clearWorkspace, boolean copyHidden, boolean filterEnabled, boolean includeFilter, String[] filters) {
    	this.path = path;
    	this.clearWorkspace = clearWorkspace;
    	this.copyHidden = copyHidden;
    	this.filterEnabled = filterEnabled;
    	this.includeFilter = includeFilter;
    	
		// in hudson 1.337, in filters = null, XStream will throw NullPointerException
		// this.filters = null;
		this.filters = new String[0];
   		if ( null != filters ) {
   			Vector<String> v = new Vector<String>();
   			for(int i=0; i<filters.length; i++) {
   				// remove empty strings
   				if ( StringUtils.isNotEmpty(filters[i]) ) {
   					v.add(filters[i]);
   				}
   			}
   			if ( v.size() > 0 ) {
   				this.filters = (String[]) v.toArray(new String[1]);
   			}
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
	
	public boolean isCopyHidden() {
		return copyHidden;
	}
	
    @Override
	public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changelogFile) 
	throws IOException, InterruptedException {
				
		long start = System.currentTimeMillis();
		PrintStream log = launcher.getListener().getLogger();
		EnvVars env = build.getEnvironment(listener);
		String expandedPath = env.expand(path);
		log.println("FSSCM.checkout " + expandedPath + " to " + workspace);
		Boolean b = Boolean.TRUE;

		AllowDeleteList allowDeleteList = new AllowDeleteList(build.getProject().getRootDir());
		
		if ( clearWorkspace ) {
			log.println("FSSCM.clearWorkspace...");
			workspace.deleteRecursive();	
		}
					
		// we will only delete a file if it is listed in the allowDeleteList
		// ie. we will only delete a file if it is copied by us
		if ( allowDeleteList.fileExists() ) {
			allowDeleteList.load();
		} else {
			// watch list save file doesn't exist
			// we will assume all existing files are under watch 
			// i.e. everything can be deleted 
			if ( workspace.exists() ) {
				// if we enable clearWorkspace on the 1st jobrun, seems the workspace will be deleted
				// running a RemoteListDir() on a not existing folder will throw an exception 
				// anyway, if the folder doesn't exist, we dont' need to list the files
				Set<String> existingFiles = workspace.act(new RemoteListDir());
				allowDeleteList.setList(existingFiles);
			}
		}
		
		RemoteFolderDiff.CheckOut callable = new RemoteFolderDiff.CheckOut();
		setupRemoteFolderDiff(callable, build.getProject(), allowDeleteList.getList(), expandedPath);
		List<FolderDiff.Entry> list = workspace.act(callable);
		
		// maintain the watch list
		for(FolderDiff.Entry entry : list) {
			if ( FolderDiff.Entry.Type.DELETED.equals(entry.getType()) ) {
				allowDeleteList.remove(entry.getFilename());
			} else {
				// added or modified
				allowDeleteList.add(entry.getFilename());
			}
		}
		allowDeleteList.save();
		
		// raw log
		String str = callable.getLog();
		if ( str.length() > 0 ) log.println(str);
		
		ChangelogSet.XMLSerializer handler = new ChangelogSet.XMLSerializer();
		ChangelogSet changeLogSet = new ChangelogSet(build, list);
		handler.save(changeLogSet, changelogFile);
		
		log.println("FSSCM.check completed in " + formatDuration(System.currentTimeMillis()-start));
		return b;
	}
	
	@Override
	public ChangeLogParser createChangeLogParser() {
		return new ChangelogSet.XMLSerializer();
	}

	/**
	 * There are two things we need to check
	 * <ul>
	 *   <li>files created or modified since last build time, we only need to check the source folder</li>
	 *   <li>file deleted since last build time, we have to compare source and destination folder</li>
	 * </ul>
	 */
	private boolean poll(AbstractProject<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener) 
	    throws IOException, InterruptedException {
		
		long start = System.currentTimeMillis();
		
		PrintStream log = launcher.getListener().getLogger();

		String expandedPath = path;

		AbstractBuild<?,?> lastCompletedBuild = project.getLastCompletedBuild();

		if (lastCompletedBuild != null){
			EnvVars env = lastCompletedBuild.getEnvironment(listener);
			expandedPath = env.expand(expandedPath);
		}

		log.println("FSSCM.pollChange: " + expandedPath);
		
		AllowDeleteList allowDeleteList = new AllowDeleteList(project.getRootDir());
		// we will only delete a file if it is listed in the allowDeleteList
		// ie. we will only delete a file if it is copied by us
		if ( allowDeleteList.fileExists() ) {
			allowDeleteList.load();
		} else {
			// watch list save file doesn't exist
			// we will assuem all existing files are under watch 
			// ie. everything can be deleted 
			Set<String> existingFiles = workspace.act(new RemoteListDir());
			allowDeleteList.setList(existingFiles);
		}
		
		RemoteFolderDiff.PollChange callable = new RemoteFolderDiff.PollChange();
		setupRemoteFolderDiff(callable, project, allowDeleteList.getList(), expandedPath);
		
		boolean changed = workspace.act(callable);
		String str = callable.getLog();
		if ( str.length() > 0 ) log.println(str);
		log.println("FSSCM.pollChange return " + changed);

		log.println("FSSCM.poolChange completed in " + formatDuration(System.currentTimeMillis()-start));		
		return changed;
	}
    private void setupRemoteFolderDiff(RemoteFolderDiff diff, AbstractProject project, Set<String> allowDeleteList){
	    setupRemoteFolderDiff(diff, project, allowDeleteList, path);
    }
	@SuppressWarnings("rawtypes")
    private void setupRemoteFolderDiff(RemoteFolderDiff diff, AbstractProject project, Set<String> allowDeleteList, String expandedPath) {
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
		
		diff.setSrcPath(expandedPath);
		
		diff.setIgnoreHidden(!copyHidden);
		
		if ( filterEnabled ) {
			if ( includeFilter ) diff.setIncludeFilter(filters);
			else diff.setExcludeFilter(filters);
		}		
		
		diff.setAllowDeleteList(allowDeleteList);
	}
		
	private static String formatDuration(long diff) {
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

    @Extension
    public static final class DescriptorImpl extends SCMDescriptor<FSSCM> {
        public DescriptorImpl() {
            super(FSSCM.class, null);
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "File System";
        }
        
        public FormValidation doFilterCheck(@QueryParameter final String value) {
        	if ( null == value || value.trim().length() == 0 ) return FormValidation.ok();
        	if ( value.startsWith("/") || value.startsWith("\\") || value.matches("[a-zA-Z]:.*") ) {
        		return FormValidation.error("Pattern can't be an absolute path");
        	} else {
        		try {
        			SimpleAntWildcardFilter filter = new SimpleAntWildcardFilter(value);
        		} catch ( Exception e ) {
        			return FormValidation.error(e, "Invalid wildcard pattern");
        		}
        	}
        	return FormValidation.ok();
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return true;
        }        
        
        @Override
        public FSSCM newInstance(StaplerRequest req, JSONObject formData) throws FormException {
        	String path = req.getParameter("fs_scm.path");
        	String[] filters = req.getParameterValues("fs_scm.filters");
        	Boolean filterEnabled = Boolean.valueOf("on".equalsIgnoreCase(req.getParameter("fs_scm.filterEnabled")));
        	Boolean includeFilter = Boolean.valueOf(req.getParameter("fs_scm.includeFilter"));
        	Boolean clearWorkspace = Boolean.valueOf("on".equalsIgnoreCase(req.getParameter("fs_scm.clearWorkspace")));
        	Boolean copyHidden = Boolean.valueOf("on".equalsIgnoreCase(req.getParameter("fs_scm.copyHidden")));
            return new FSSCM(path, clearWorkspace, copyHidden, filterEnabled, includeFilter, filters);
        }
        
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(AbstractBuild<?, ?> build,
            Launcher launcher, TaskListener listener) throws IOException,
            InterruptedException {
        // we cannot really calculate a sensible revision state for a filesystem folder
        // therefore we return NONE and simply ignore the baseline in compareRemoteRevisionWith
        return SCMRevisionState.NONE;
    }

    @Override
    protected PollingResult compareRemoteRevisionWith(
            AbstractProject<?, ?> project, Launcher launcher,
            FilePath workspace, TaskListener listener, SCMRevisionState baseline)
            throws IOException, InterruptedException {
        
        if(poll(project, launcher, workspace, listener)) {
            return PollingResult.SIGNIFICANT;
        } else {
            return PollingResult.NO_CHANGES;
        }
    }

}
