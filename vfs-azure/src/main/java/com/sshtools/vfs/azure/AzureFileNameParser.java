package com.sshtools.vfs.azure;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.HostFileNameParser;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponentContext;

public class AzureFileNameParser extends HostFileNameParser {

	private static final AzureFileNameParser instance = new AzureFileNameParser();

	public static AzureFileNameParser getInstance() {
		return instance;
	}

	public AzureFileNameParser() {
		super(443);
	}

	public FileName parseUri(final VfsComponentContext context, FileName base, String filename)
			throws FileSystemException {
		final StringBuilder name = new StringBuilder();
		Authority auth = null;
		String path = null;
		FileType fileType;

		int eidx = filename.indexOf("@/");
		if (eidx != -1) 
			filename = filename.substring(0,  eidx + 1) + "windowsazure.com" + filename.substring(eidx + 1);
		
		try {
			auth = extractToPath(filename, name);
			if (auth.getUserName() == null) {
				UriParser.extractScheme(filename, name);
				UriParser.canonicalizePath(name, 0, name.length(), this);
				UriParser.fixSeparators(name);
			}
			fileType = UriParser.normalisePath(name);
			path = name.toString();
			if (path.equals("")) {
				path = "/";
			}
		} catch (FileSystemException fse) {
			UriParser.extractScheme(filename, name);
			UriParser.canonicalizePath(name, 0, name.length(), this);
			UriParser.fixSeparators(name);
			// final String rootFile = extractRootPrefix(filename, name);
			fileType = UriParser.normalisePath(name);
			path = name.toString();

		}
		return new AzureFileName(auth == null ? null : auth.getUserName(), auth == null ? null : auth.getPassword(),
				path, fileType);
	}

}
