package hudson.plugin.scm.fsscm;

import static org.junit.Assert.*;
import org.junit.*;
import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import org.apache.commons.lang.time.DurationFormatUtils;;

public class FolderDiffTest2 {

	@Test
	public void testGetRelative1() throws IOException {	
		String x = FolderDiff.getRelativeName("c:\\tmp\\abc\\qq\\qq.java", "c:\\tmp");
		assertEquals(x, "abc\\qq\\qq.java");
	}
	
	@Test
	public void testGetRelative2() throws IOException {
		String x = FolderDiff.getRelativeName("c:\\tmp\\abc\\qq\\qq.java", "c:\\");
		assertEquals(x, "tmp\\abc\\qq\\qq.java");
	}
	
	@Test(expected=IOException.class)
	public void testGetRelative3() throws IOException {
		String x = FolderDiff.getRelativeName("c:\\tmp\\abc\\qq\\qq.java", "c:\\tm");
	}
	
	@Test(expected=IOException.class)
	public void testGetRelative4() throws IOException {
		String x = FolderDiff.getRelativeName("c:\\tmp\\abc\\qq\\qq.java", "c:\\def");
	}
		
	@Test(expected=IOException.class)
	public void testGetRelative5() throws IOException {
		String x = FolderDiff.getRelativeName("c:\\tmp\\abc\\qq\\qq.java", "c:\\tmp//");
	}

}
