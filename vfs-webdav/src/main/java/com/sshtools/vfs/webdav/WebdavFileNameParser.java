package com.sshtools.vfs.webdav;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.FileNameParser;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponentContext;
import org.apache.commons.vfs2.provider.http.HttpFileNameParser;


/**
 * Borrowed from standard implem (there seems to be some mess with default port
 * 80 ...)
 * 
 * @author ndx
 * 
 */
public class WebdavFileNameParser extends HttpFileNameParser {
	private static final WebdavFileNameParser INSTANCE = new WebdavFileNameParser();

	public WebdavFileNameParser() {
		super();
	}

	public static FileNameParser getInstance() {
		return INSTANCE;
	}

	@Override
	public FileName parseUri(final VfsComponentContext context, FileName base, final String filename) throws FileSystemException {
		// FTP URI are generic URI (as per RFC 2396)
		final StringBuilder name = new StringBuilder();

		// Extract the scheme and authority parts
		final Authority auth = extractToPath(context, filename, name);

		// Extract the queryString
		String queryString = UriParser.extractQueryString(name);

		// Decode and normalise the file name
		UriParser.canonicalizePath(name, 0, name.length(), this);
		UriParser.fixSeparators(name);
		FileType fileType = UriParser.normalisePath(name);
		final String path = name.toString();

		return new URLFileName(auth.getScheme(), auth.getHostName(), auth.getPort(), getDefaultPort(), auth.getUserName(), auth.getPassword(), path, fileType,
						queryString);
	}
}
