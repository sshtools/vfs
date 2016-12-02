package com.sshtools.vfs.nfs;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.HostFileNameParser;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponentContext;

public class NFSFileNameParser extends HostFileNameParser {

	public NFSFileNameParser() {
		super(0);
	}

	@Override
	public FileName parseUri(final VfsComponentContext context, final FileName base, String filename)
			throws FileSystemException {
		final StringBuilder name = new StringBuilder(filename);

		String mount = UriParser.extractQueryString(name);
		if (mount != null && (mount.equals("/") || mount.equals("")))
			mount = null;
		if(mount == null && base != null && base instanceof NFSFileName)
			mount = ((NFSFileName)base).getMount();
		
		filename = name.toString();
		name.setLength(0);

		// Extract the scheme and authority parts
		final Authority auth = extractToPath(filename, name);

		// Decode and normalise the file name
		UriParser.canonicalizePath(name, 0, name.length(), this);
		UriParser.fixSeparators(name);
		final FileType fileType = UriParser.normalisePath(name);
		final String path = name.toString();

		return new NFSFileName(auth.getScheme(), auth.getHostName(), mount, auth.getUserName(), auth.getPassword(),
				path, fileType);
	}
}
