package com.sshtools.vfs.dropbox;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileNameParser;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponentContext;

public class DropboxFileNameParser extends AbstractFileNameParser {
	static DropboxFileNameParser instance = new DropboxFileNameParser();

	public static DropboxFileNameParser getInstance() {
		return instance;
	}

	@Override
	public FileName parseUri(VfsComponentContext context, FileName base, String filename) throws FileSystemException {
		StringBuilder name = new StringBuilder();
		String scheme = UriParser.extractScheme(filename, name);
		// Remove ://
		name.delete(0, 2);
		UriParser.canonicalizePath(name, 0, name.length(), this);
		// Normalize separators in the path
		UriParser.fixSeparators(name);
		// Normalise the path
		FileType fileType = UriParser.normalisePath(name);
		return new DropboxFileName(scheme, name.toString(), fileType);
	}
}
