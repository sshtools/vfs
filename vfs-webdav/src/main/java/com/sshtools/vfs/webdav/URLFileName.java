package com.sshtools.vfs.webdav;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.GenericFileName;
import org.apache.commons.vfs2.provider.UriParser;

/**
 * @author <a href="http://commons.apache.org/vfs/team-list.html">Commons VFS
 *         team</a>
 */
public class URLFileName extends GenericFileName {
	private static final char[] TMP_RESERVED_CHARS = new char[] { '?', '\\', ' ', '&', '"', '\'', '*', '#', ';', ':',
			'<', '>', '|' };

	private static final int BUFFSZ = 250;

	private final String queryString;

	public URLFileName(final String scheme, final String hostName, final int port, final int defaultPort,
			final String userName, final String password, final String path, final FileType type,
			final String queryString) {
		super(scheme, hostName, port, defaultPort, userName, password, path, type);
		this.queryString = queryString;
	}

	/**
	 * Get the query string.
	 *
	 * @return the query string part of the filename
	 */
	public String getQueryString() {
		return queryString;
	}

	/**
	 * Get the path and query string e.g. /path/servlet?param1=true.
	 *
	 * @return the path and its query string
	 */
	public String getPathQuery() {
		StringBuilder sb = new StringBuilder(BUFFSZ);
		sb.append(getPath());
		sb.append("?");
		sb.append(getQueryString());

		return sb.toString();
	}

	/**
	 * Create a FileName.
	 * 
	 * @param absPath
	 *            The absolute path.
	 * @param type
	 *            The FileType.
	 * @return The FileName
	 */
	@Override
	public FileName createName(final String absPath, FileType type) {
		return new URLFileName(getScheme(), getHostName(), getPort(), getDefaultPort(), getUserName(), getPassword(),
				absPath, type, getQueryString());
	}

	/**
	 * Append query string to the uri.
	 *
	 * @return the uri
	 */
	@Override
	protected String createURI() {
		if (getQueryString() != null) {
			StringBuilder sb = new StringBuilder(BUFFSZ);
			sb.append(super.createURI());
			sb.append("?");
			sb.append(getQueryString());

			return sb.toString();
		}

		return super.createURI();
	}

	/**
	 * Encode a URI.
	 * 
	 * @param charset
	 *            The character set.
	 * @return The encoded URI
	 * @throws FileSystemException
	 *             if some other exception occurs.
	 */
	public String getURIEncoded(String charset) throws FileSystemException {
		if (charset != null) {
			throw new UnsupportedOperationException("non null charset \"" + charset + "\" not yet supported");
		}
		final StringBuilder buffer = new StringBuilder();
		appendRootUri(buffer, true);
		buffer.append(UriParser.encode(getPath(), TMP_RESERVED_CHARS));
		return buffer.toString();
	}
}
