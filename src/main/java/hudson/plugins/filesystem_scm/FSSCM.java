package hudson.plugins.filesystem_scm;

import java.io.*;
import java.util.*;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.filesystem_scm.ChangelogSet.XMLSerializer;
import hudson.scm.ChangeLogParser;
import hudson.scm.PollingResult;
import hudson.scm.SCMRevisionState;
import hudson.scm.SCM;
import hudson.scm.SCMDescriptor;
import hudson.util.FormValidation;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

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

    private transient boolean copyHidden;
    /**
     * If we have include/exclude filter, then this is true.
     *
     * @deprecated Moved to {@link FilterSettings}
     */
    @Deprecated
    private transient boolean filterEnabled;
    /**
     * Is this filter a include filter or exclude filter
     *
     * @deprecated Moved to {@link FilterSettings}
     */
    @Deprecated
    private transient boolean includeFilter;
    /**
     * filters, which will be passed to {@link WildcardFileFilter}.
     *
     * @deprecated Moved to {@link FilterSettings}
     */
    @Deprecated
    private String[] filters;

    /**
     * Filter settings.
     *
     * @since TODO
     */
    @CheckForNull
    private FilterSettings filterSettings;

    @DataBoundConstructor
    public FSSCM(String path, boolean clearWorkspace, boolean copyHidden, FilterSettings filterSettings) {
        this.path = path;
        this.clearWorkspace = clearWorkspace;
        this.copyHidden = copyHidden;
        this.filterSettings = filterSettings;
    }

    @Deprecated
    public FSSCM(String path, boolean clearWorkspace, boolean copyHidden, boolean filterEnabled, boolean includeFilter, String[] filters) {
        this.path = path;
        this.clearWorkspace = clearWorkspace;
        this.copyHidden = copyHidden;

        if (filterEnabled) {
            List<FilterSelector> selectors = new ArrayList<>();
            if (null != filters) {
                for (String filter : filters) {
                    // remove empty strings
                    if (StringUtils.isNotEmpty(filter)) {
                        selectors.add(new FilterSelector(filter));
                    }
                }
            }
            filterSettings = new FilterSettings(includeFilter, selectors);
        }
    }
    
    public String getPath() {
        return path;
    }

    /**
     * @deprecated Use {@link #getFilterSettings()}.
     */
    @CheckForNull
    public String[] getFilters() {
        if (filterSettings == null) {
            return null;
        }
        final List<String> wildcards = filterSettings.getWildcards();
        return wildcards.toArray(new String[wildcards.size()]);
    }
	
	public boolean isFilterEnabled() {
		return filterSettings != null;
	}
	
	public boolean isIncludeFilter() {
		return filterSettings != null && filterSettings.isIncludeFilter();
	}
	
	public boolean isClearWorkspace() {
		return clearWorkspace;
	}
	
	public boolean isCopyHidden() {
		return copyHidden;
	}

    @CheckForNull
    public FilterSettings getFilterSettings() {
        return filterSettings;
    }

    protected Object readResolve() {
        if (filterEnabled && filterSettings == null) {
            final List<FilterSelector> selectors;
            if (filters != null) {
                selectors = new ArrayList<>(filters.length);
                for (String value : filters) {
                    selectors.add(new FilterSelector(value));
                }
            } else {
                selectors = Collections.emptyList();
            }
            filterSettings = new FilterSettings(includeFilter, selectors);
        }
        return this;
    }

    @Override
    public void checkout(Run<?, ?> build, Launcher launcher, FilePath workspace, 
            TaskListener listener, File changelogFile, SCMRevisionState baseline) 
            throws IOException, InterruptedException {

				
		long start = System.currentTimeMillis();
		PrintStream log = launcher.getListener().getLogger();
		log.println("FSSCM.checkout " + path + " to " + workspace);

		AllowDeleteList allowDeleteList = new AllowDeleteList(build.getParent().getRootDir());
		
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
		setupRemoteFolderDiff(callable, build.getParent(), allowDeleteList.getList());
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
		
		processChangelog(build, changelogFile, list);
		
		log.println("FSSCM.check completed in " + formatDuration(System.currentTimeMillis()-start));
	}

	protected void processChangelog(Run<?, ?> build, File changelogFile, List<FolderDiff.Entry> list)
			throws FileNotFoundException {
		// checking for null as the @CheckForNull Annotation @asks for by SCM.checkout 
		if(changelogFile!=null) {
			ChangelogSet.XMLSerializer serializer = createXMLSerializer();
			ChangelogSet changeLogSet = new ChangelogSet(build, list);
			serializer.save(changeLogSet, changelogFile);
		}
	}

	private XMLSerializer createXMLSerializer() {
		return new ChangelogSet.XMLSerializer();
	}
	
	@Override
	public ChangeLogParser createChangeLogParser() {
		return createXMLSerializer();
	}

	/**
	 * There are two things we need to check
	 * <ul>
	 *   <li>files created or modified since last build time, we only need to check the source folder</li>
	 *   <li>file deleted since last build time, we have to compare source and destination folder</li>
	 * </ul>
	 */
	private boolean poll(Job<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener) 
	    throws IOException, InterruptedException {
		
		long start = System.currentTimeMillis();
		
		PrintStream log = launcher.getListener().getLogger();
		log.println("FSSCM.pollChange: " + path);
		
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
		setupRemoteFolderDiff(callable, project, allowDeleteList.getList());
		
		boolean changed = workspace.act(callable);
		String str = callable.getLog();
		if ( str.length() > 0 ) log.println(str);
		log.println("FSSCM.pollChange return " + changed);

		log.println("FSSCM.poolChange completed in " + formatDuration(System.currentTimeMillis()-start));		
		return changed;
	}
	
	@SuppressWarnings("rawtypes")
    private void setupRemoteFolderDiff(RemoteFolderDiff diff, Job<?, ?> project, Set<String> allowDeleteList) {
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
		
		diff.setIgnoreHidden(!copyHidden);
		
		if ( filterSettings != null ) {
			if ( filterSettings.isIncludeFilter() )  {
                            diff.setIncludeFilter(getFilters());
                        } else { 
                            diff.setExcludeFilter(getFilters());
                        }
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
    @Symbol("filesystem")
    public static final class DescriptorImpl extends SCMDescriptor<FSSCM> {
        public DescriptorImpl() {
            super(FSSCM.class, null);
            load();
        }
        
        @Override
        public String getDisplayName() {
            return "File System";
        }
        
        /**
         * @deprecated Use {@link FilterSelector.DescriptorImpl#doCheckWildcard(java.lang.String)}
         */
        @Deprecated
        @Restricted(NoExternalUse.class)
        public FormValidation doFilterCheck(@QueryParameter final String value) {
            return Jenkins.getActiveInstance()
                    .getDescriptorByType(FilterSelector.DescriptorImpl.class)
                    .doCheckWildcard(value);
        }
        
        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            return true;
        }

		@Override
		public boolean isApplicable(Job project) {
        	// All job types are supported, the plugin does not depend on AbstractProject/AbstractBuild anymore
			return true;
		}
	}

    @Override
    public SCMRevisionState calcRevisionsFromBuild(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
        // we cannot really calculate a sensible revision state for a filesystem folder
        // therefore we return NONE and simply ignore the baseline in compareRemoteRevisionWith
        return SCMRevisionState.NONE;
    }

    @Override
    public PollingResult compareRemoteRevisionWith(Job<?, ?> project, Launcher launcher, FilePath workspace, TaskListener listener, SCMRevisionState baseline) throws IOException, InterruptedException {
        if(poll(project, launcher, workspace, listener)) {
            return PollingResult.SIGNIFICANT;
        } else {
            return PollingResult.NO_CHANGES;
        }
    }
}
