package hudson.plugins.filesystem_scm;

import java.io.File;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.AbstractFileFilter;

public class SimpleAntWildcardFilter extends AbstractFileFilter {
	
	private Pattern wildcardPattern;
	
	public SimpleAntWildcardFilter(String antWildcard) {
		String line = antWildcard;
		line = line.replace('\\', '/');
		String[] list = line.split("/");
		StringBuilder buf = new StringBuilder();
		for(int i=0; i<list.length; i++) {
			String s = list[i];
			if ( "**".equals(s) ) {
				buf.append("(/.*)?");
			} else {
				s = s.replace(".", "\\.");
				s = s.replace('?', '.');
				s = s.replace("*", "[^\\/]*");
				if ( buf.length() == 0 ) {
					if ( s.length() == 0 || '/' == s.charAt(0) || s.matches("[a-zA-Z]\\:") ) {
						// this is absolute path
						// the search will be "^xxxx$", full match
						buf.append('^');
					} else {
						// this is not absolute path
						// the match will be "xxxx$", tail match only
						// but we still need to prefix with a '/'
						buf.append('/');
					}
				} else {
					buf.append('/');
				}
				buf.append(s);
			}
		}
		buf.append('$');
		String regexStr = buf.toString();
		//System.out.println(antWildcard);
		//System.out.println(regexStr);
		wildcardPattern = Pattern.compile(regexStr, Pattern.CASE_INSENSITIVE);		
	}
	
	public Pattern getPattern() {
		return wildcardPattern;
	}
	
	public boolean accept(File file) {
		//System.out.println("accept(" + file + ")");
		if ( file.isDirectory() ) return true; 
		String fullPath = file.getAbsolutePath();
		fullPath = fullPath.replace('\\', '/');
		return wildcardPattern.matcher(fullPath).find();
	}

}
