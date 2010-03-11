package hudson.plugins.filesystem_scm;

import hudson.model.*;
import hudson.util.XStream2;
import java.util.*;
import java.io.*;
import org.apache.commons.io.IOUtils;

/** FileSystem base SCM ChangelogSet
 * <p>Not like other SCMs, there is always just ONE set of changelog when we checkout. 
 * While multiple users may have modified some files between two builds, but we will only be 
 * able to detect if there is any files modified (YES or NO).</p>
 * 
 * <p>XML serialization is done by XStream2</p>
 * 
 * @author Sam NG
 *
 */
public class ChangelogSet extends hudson.scm.ChangeLogSet {

	// I'm FileSystem SCM, basically I will only have 1 changelog
	// not like other SCM, e.g. SVN, there may be 2 or 3 committed changes between builds
	private List<Changelog> logs;
	
	public ChangelogSet(AbstractBuild build, List<FolderDiff.Entry> changes) {
		super(build);
		logs = new ArrayList<Changelog>();
		logs.add(new Changelog(this, changes));
	}
	
	@Override
	public String getKind() {
		return "fs_scm";
	}
		
	@Override
	public boolean isEmptySet() {
		return logs.isEmpty();
	}

	public Iterator<Changelog> iterator() {
		return Collections.unmodifiableList(logs).iterator();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((logs == null) ? 0 : logs.hashCode());
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
		final ChangelogSet other = (ChangelogSet) obj;
		if (logs == null) {
			if (other.logs != null)
				return false;
		} else if (!logs.equals(other.logs))
			return false;
		return true;
	}	

	public static class XMLSerializer extends hudson.scm.ChangeLogParser {
		private transient XStream2 xstream;
		private Object readResolve() {  // xstream field used to be serialized in build.xml
			initXStream();
			return this;
		}
		
		public XMLSerializer() {
			initXStream();
		}

		private void initXStream() {
			xstream = new XStream2();
			xstream.alias("log", ChangelogSet.class);
			//xstream.addImplicitCollection(ChangelogSet.class, "changeLogSet");
			xstream.aliasField("changelogset", ChangelogSet.class, "changeLogSet");
			xstream.alias("changelog", Changelog.class);
			xstream.alias("path", Changelog.Path.class);
			xstream.omitField(hudson.scm.ChangeLogSet.class, "build");
			//xstream.omitField(ChangelogSet.ChangeLog.class, "parent");
			//xstream.omitField(ChangelogSet.Path.class, "changeLog");
		}
		
		public ChangelogSet parse(AbstractBuild build, java.io.File file) throws FileNotFoundException {
			FileInputStream in = null;
			ChangelogSet out = null;
			try {
				in = new FileInputStream(file);
				out = (ChangelogSet)xstream.fromXML(in);
			} finally {
				IOUtils.closeQuietly(in);
			}
			return out;
		}
		
		public void save(ChangelogSet changeLogSet, File file) throws FileNotFoundException {
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(file);
				xstream.toXML(changeLogSet, out);
			} finally {
				IOUtils.closeQuietly(out);
			}
		}
	}
}
