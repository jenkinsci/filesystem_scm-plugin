package hudson.plugins.filesystem_scm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import hudson.plugins.filesystem_scm.FolderDiff.Entry;

public class FSSCMTest {

    @TempDir
    public File testFolder;
	
	FSSCM fsscm = new FSSCM("", false, false, null);
	List<Entry> list = new ArrayList<>();

    @Test
    void processChangelog_nullChangelogFile_NoException() throws FileNotFoundException {
		fsscm.processChangelog(null, null, list);
	}

    @Test
    void processChangelog_ChangelogFile_createdChanelogFile() throws FileNotFoundException {
		File changeLogFile = new File(testFolder,"changelog.xml");
		assertFalse(changeLogFile.exists());
		fsscm.processChangelog(null, changeLogFile, list);
		assertTrue(changeLogFile.exists());
	}
}
