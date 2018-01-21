package hudson.plugins.filesystem_scm;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import hudson.plugins.filesystem_scm.FolderDiff.Entry;

public class FSSCMTest {
	
	@Rule
    public TemporaryFolder testFolder = new TemporaryFolder();
	
	FSSCM fsscm = new FSSCM("", false, false, null);
	List<Entry> list = new ArrayList<>();
	
	@Test
	public void processChangelog_nullChangelogFile_NoException() throws FileNotFoundException {
		fsscm.processChangelog(null, null, list);
	}
	
	@Test
	public void processChangelog_ChangelogFile_createdChanelogFile() throws FileNotFoundException {
		File changeLogFile = new File(testFolder.getRoot(),"changelog.xml");
		Assert.assertFalse(changeLogFile.exists());
		fsscm.processChangelog(null, changeLogFile, list);
		Assert.assertTrue(changeLogFile.exists());
	}
}
