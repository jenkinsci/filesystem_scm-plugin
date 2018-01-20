package hudson.plugins.filesystem_scm;

import static org.junit.Assert.*;
import org.junit.*;
import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import org.apache.commons.lang.time.DurationFormatUtils;;

public class FolderDiffTest2 {
	
	private static String createPlatformDependendPath(String... parts) {
		String path = "";
		for (int i=0; i<parts.length-1; i++) {
			path = path.concat(parts[i]).concat(File.separator);
		}
		path = path.concat(parts[parts.length-1]);
		return path;
	}

	@Test
	public void GetRelative_correctFileAndFolder_Subpath() throws IOException {
		String folder = createPlatformDependendPath("c:","tmp");
		String expected = createPlatformDependendPath("abc","qq","qq.java");
		String actual = FolderDiff.getRelativeName(createPlatformDependendPath(folder,expected), folder);
		assertEquals(expected, actual);
	}
	
	@Test
	public void GetRelative_correctFileAndDrive_Subpath() throws IOException {
		String folder = createPlatformDependendPath("c:");
		String expected = createPlatformDependendPath("tmp","abc","qq","qq.java");
		String actual = FolderDiff.getRelativeName(createPlatformDependendPath(folder,expected), folder);
		assertEquals(expected, actual);
	}
	
	@Test(expected=IOException.class)
	public void GetRelative_FileAndFolderWithALetterMissingInName_Exception() throws IOException {
		String folder = createPlatformDependendPath("c:","tm");
		String actual = FolderDiff.getRelativeName(createPlatformDependendPath("c:","tmp","abc","qq","qq.java"), folder);
	}
	
	@Test(expected=IOException.class)
	public void GetRelative_FileAndFolderNotParentOfFile_Exception() throws IOException {
		String folder = createPlatformDependendPath("c:","def"); 
		String file = createPlatformDependendPath("c:","tmp","abc","qq","qq.java");
		String x = FolderDiff.getRelativeName(file, folder);
	}
		
	@Test(expected=IOException.class)
	public void GetRelative_FileAndFolderWithPathSeperatorAppended_Exception() throws IOException {
		String folder = createPlatformDependendPath("c:","tmp");
		String expected = createPlatformDependendPath("abc","qq","qq.java");
		String actual = FolderDiff.getRelativeName(createPlatformDependendPath(folder,expected), folder.concat("//"));
	}
}
