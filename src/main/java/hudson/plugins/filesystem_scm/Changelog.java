package hudson.plugins.filesystem_scm;

import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/** Represents a Changelog record (ChangeLogSet.Entry) in ChangelogSet
 * 
 * <p>Author is always "Unknown"</p>
 * <p>Date is always the checkout datetime</p>
 * 
 *@author Sam NG
 *
 */
public class Changelog extends hudson.scm.ChangeLogSet.Entry {
	
	private ChangelogSet parent;
	private Date date;
	private List<Path> paths;
	
	public Changelog() {
		// do nothing, only for serialization
	}
	
	public Changelog(ChangelogSet parent) {
		this.parent = parent;
	}
	
	public Changelog(ChangelogSet parent, List<FolderDiff.Entry> changes) {
		this.parent = parent;
		
		paths = new ArrayList<Path>();
		for(int i=0; i<changes.size(); i++) {
			FolderDiff.Entry change = changes.get(i);
			Path path = new Path(this, change);
			paths.add(path);
		}
	}
	
	@Override
	public Collection<String> getAffectedPaths() {
		ArrayList<String> list = new ArrayList<String>();
		for( Path path : paths ) {
			list.add(path.getValue());
		}
		return Collections.unmodifiableList(list);
	}
	
	@Override
	public Collection<Path> getAffectedFiles() {
		return Collections.unmodifiableList(paths);
	}
	
	@Override
	public String getMsg() {
		if ( 0 == paths.size() ) return "No change";
		
		int add = 0;
		int del = 0;
		int edit = 0;
		for(int i=0; i<paths.size(); i++) {
			Path path = paths.get(i);
			if ( "ADD".equalsIgnoreCase(path.action) ) ++add;
			else if ( "DELETE".equalsIgnoreCase(path.action) ) ++del;
			else ++edit;
		}
		StringBuffer buf = new StringBuffer();
		buildMessage(buf, add, "new file", "new files");
		buildMessage(buf, edit, "file modified", "files modified");
		buildMessage(buf, del, "file deleted", "files deleted");
		return buf.toString();
	}
	
	private void buildMessage(StringBuffer buf, int count, String singular, String plural) {
		if ( count > 0 ) {
			if ( buf.length() > 0 ) buf.append(", ");
			buf.append(count).append(' ');
			if ( count > 1 ) buf.append(plural);
			else buf.append(singular);
		}
		return;
	}
	
	@Override
	public hudson.model.User getAuthor() {
		return hudson.model.User.getUnknown();
	}
			
	@Override
	public ChangeLogSet getParent() {
		return parent;
	}
	
	@Override
	protected void setParent(ChangeLogSet parent) {
		this.parent = (ChangelogSet)parent;
	}

	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((date == null) ? 0 : date.hashCode());
		result = prime * result + ((paths == null) ? 0 : paths.hashCode());
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
		final Changelog other = (Changelog) obj;
		if (date == null) {
			if (other.date != null)
				return false;
		} else if (!date.equals(other.date))
			return false;
		if (paths == null) {
			if (other.paths != null)
				return false;
		} else if (!paths.equals(other.paths))
			return false;
		return true;
	}
	
	/** A changed file in Changelog
	 * 
	 * @author Sam NG
	 *
	 */
	public static class Path implements hudson.scm.ChangeLogSet.AffectedFile {
		
		/** The filepath of the modified file
		 * 
		 */
		private String value;
		
		/** Either "ADD", "DELETE" or "EDIT"
		 * 
		 */
		private String action;
		
		/** The parent changelog object this child belongs to
		 * 
		 */
		private Changelog changelog;
		
		public Path() {
			// do nothing, only for serialization
		}
		
		public Path(Changelog changelog) {
			this.changelog = changelog;
		}
		
		public Path(Changelog changelog, FolderDiff.Entry entry) {
			this.changelog = changelog;
			setValue(entry.getFilename());
			if ( FolderDiff.Entry.Type.NEW == entry.getType() ) setAction("ADD");
			else if ( FolderDiff.Entry.Type.DELETED == entry.getType() ) setAction("DELETE");
			else setAction("EDIT");
		}
		
		/**
		 * Inherited from AffectedFile
		 */
		public String getPath() {
			return getValue();
		}
		
		public String getValue() {
			return value;
		}
		
		public void setValue(String value) {
			this.value = value;
		}
		
		public String getAction() {
			return action;
		}
		
		public void setAction(String action) {
			this.action = action;
		}
		
		public Changelog getChangelog() {
			return changelog;
		}
		
		protected void setChangelog(Changelog changelog) {
			this.changelog = changelog;
		}
		
        public hudson.scm.EditType getEditType()
        {
            if( "ADD".equalsIgnoreCase(action) ) return EditType.ADD;
            else if( "DELETE".equalsIgnoreCase(action) ) return EditType.DELETE;
            else return EditType.EDIT;
        }

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((action == null) ? 0 : action.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
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
			final Path other = (Path) obj;
			if (action == null) {
				if (other.action != null)
					return false;
			} else if (!action.equals(other.action))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}		
	}			
}