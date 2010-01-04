package hudson.plugins.filesystem_scm;

import java.io.*;
import java.util.*;

import org.apache.commons.io.IOUtils;

/** We will only delete file from workspace if it is in the allowDeleteList.
 * 
 * <p>And each time we add any files to the workspace, we will add a record in this list.
 * 
 */
public class AllowDeleteList {
	
	final private static String ALLOW_DELETE_LIST_BASENAME = "fsscm_allow_delete_list.dat";

	private File file;
	private Set<String> set;
	
	public AllowDeleteList(File projectPath) {
		file = new File(projectPath, ALLOW_DELETE_LIST_BASENAME);
		set = new HashSet<String>();
	}
	
	public boolean fileExists() {
		return file.exists();
	}
	
	public Set<String> getList() {
		return Collections.unmodifiableSet(set);
	}
	
	public void setList(Set<String> list) {
		set.clear();
		set.addAll(list);
	}
	
	public boolean add(String item) {
		return set.add(item);
	}
	
	public boolean remove(String item) {
		return set.remove(item);
	}
	
	public void save() throws IOException {
		PrintStream out = null;
		try {
			out = new PrintStream(file, "UTF-8");
			for(String name : set) {
				out.println(name);
			}
		} finally {
			IOUtils.closeQuietly(out);
		}		
	}
	
	public void load() throws IOException {
		set.clear();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			while(reader.ready()) {
				String line = reader.readLine();
				if ( null != line && line.length() > 0 ) {
					set.add(line);
				}
			}
		} finally {
			IOUtils.closeQuietly(reader);
		}		
	}
}
