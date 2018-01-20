package hudson.plugins.filesystem_scm;

import hudson.model.Run;
import java.util.*;
import org.junit.*;
import java.io.*;
import static org.junit.Assert.*;

public class ChangelogSetXMLTest {

	ChangelogSet changeLogSet;
	
	@Before
	public void setupChangeLogSet() {
		List<FolderDiff.Entry> changes = new ArrayList<FolderDiff.Entry>();
		changes.add(new FolderDiff.Entry("c:\\tmp\\del.java", FolderDiff.Entry.Type.DELETED));
		changes.add(new FolderDiff.Entry("c:\\tmp\\add.java", FolderDiff.Entry.Type.NEW));
		changes.add(new FolderDiff.Entry("c:\\tmp\\edit.java", FolderDiff.Entry.Type.MODIFIED));
		changes.add(new FolderDiff.Entry("c:\\tmp\\cc.java", FolderDiff.Entry.Type.MODIFIED));
		changeLogSet = new ChangelogSet(null, changes);
	}
	
	@Test
	public void testToAndFromXML() throws IOException {
		ChangelogSet.XMLSerializer handler = new ChangelogSet.XMLSerializer(); 
		File tmp = File.createTempFile("xstream", null);
		
		handler.save(changeLogSet, tmp);
		
		ChangelogSet out = handler.parse((Run)null, tmp);

		assertEquals(changeLogSet, out);
	}
	
	@Test
	public void save_nullFile_noException() throws FileNotFoundException {
		ChangelogSet.XMLSerializer handler = new ChangelogSet.XMLSerializer(); 
		File changelogFile = null;
		handler.save(changeLogSet, changelogFile);
	}
}
