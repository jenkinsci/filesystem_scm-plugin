package hudson.plugins.filesystem_scm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.xml.sax.SAXException;

import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import hudson.util.XStream2;

/**
 * FileSystem base SCM ChangelogSet
 * <p>
 * Unlike other SCMs, there is at most ONE set of changelog when we checkout.
 * While multiple users may have modified some files between two builds, but we
 * will only be able to detect if there is any files modified (YES or NO).
 * </p>
 * 
 * <p>
 * XML serialization is done by XStream2
 * </p>
 * 
 * @author Sam NG
 *
 */
public class ChangelogSet extends hudson.scm.ChangeLogSet<Changelog> {

    // I'm FileSystem SCM, basically I will only have 1 changelog
    // not like other SCM, e.g. SVN, there may be 2 or 3 committed changes between
    // builds
    private List<Changelog> logs;

    public ChangelogSet(Run<?, ?> build, List<FolderDiff.Entry> changes) {
        super(build, new FilesystemRepositoryBrowser());
        logs = new ArrayList<>();
        if (!changes.isEmpty()) {
            logs.add(new Changelog(this, changes));
        }
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
        result = prime * result + ((logs == null) ? 0 : logs.hashCode());
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
        private static final XStream2 xstream;
        static {
            xstream = new XStream2();
            xstream.alias("log", ChangelogSet.class);
            // xstream.addImplicitCollection(ChangelogSet.class, "changeLogSet");
            xstream.aliasField("changelogset", ChangelogSet.class, "changeLogSet");
            xstream.alias("changelog", Changelog.class);
            xstream.alias("path", Changelog.Path.class);
            xstream.omitField(hudson.scm.ChangeLogSet.class, "build");
            // xstream.omitField(ChangelogSet.ChangeLog.class, "parent");
            // xstream.omitField(ChangelogSet.Path.class, "changeLog");
        }

        @Override
        public ChangeLogSet<? extends Entry> parse(Run build, RepositoryBrowser<?> browser, File changelogFile)
                throws IOException, SAXException {
            return parse(build, changelogFile);
        }

        @SuppressWarnings("rawtypes")
        public ChangelogSet parse(Run<?, ?> build, java.io.File file) throws FileNotFoundException {
            FileInputStream in = null;
            ChangelogSet out = null;
            try {
                in = new FileInputStream(file);
                out = (ChangelogSet) xstream.fromXML(in);
            } finally {
                IOUtils.closeQuietly(in);
            }
            return out;
        }

        public void save(ChangelogSet changeLogSet, File changelogFile) throws FileNotFoundException {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(changelogFile);
                xstream.toXML(changeLogSet, out);
            } finally {
                IOUtils.closeQuietly(out);
            }
        }
    }
}
