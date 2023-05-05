package com.sshtools.jafp.client;

public class AFPUtil {
	public static String removeTrailingSeparator(String path) {
		while(path.endsWith("/") && path.length() > 1)
			path = path.substring(0,  path.length() - 1);
		return path;
	}

	public static String appendPaths(String left, String right) {
		StringBuilder b = new StringBuilder();
		left = removeTrailingSeparator(left);
		b.append(left);
		if(!left.endsWith("/"))
			b.append('/');
		b.append(right);
		return b.toString();
	}
	
	public static String removeLeadingSlash(String path) {
		while(path.startsWith("/") && path.length() > 1)
			path = path.substring(1);
		return path;
	}
}
