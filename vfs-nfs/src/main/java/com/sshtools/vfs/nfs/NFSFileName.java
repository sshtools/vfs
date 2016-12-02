package com.sshtools.vfs.nfs;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.UriParser;

public class NFSFileName extends AbstractFileName {

	private static final char[] USERNAME_RESERVED = { ':', '@', '/' };
	private static final char[] PASSWORD_RESERVED = { '@', '/', '?' };
	private static final int BUFFER_SIZE = 250;

	private String mount;
	private String decodedAbsPath;
	private String hostName;
	private String userName;
	private String password;

	NFSFileName(String scheme, String hostName, String mount, String userName, String password, String path,
			FileType type) {
		super(scheme, path, type);
		this.mount = mount;
		this.hostName = hostName;
		this.userName = userName;
		this.password = password;
	}

	public String getHostName() {
		return hostName;
	}

	public String getUserName() {
		return userName;
	}

	public String getPassword() {
		return password;
	}

	public String getMount() {
		return mount;
	}

	public String getMountDecoded() throws FileSystemException {
		if (decodedAbsPath == null) {
			decodedAbsPath = UriParser.decode(getMount());
		}

		return decodedAbsPath;
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
		return new NFSFileName(getScheme(), getHostName(), getMount(), getUserName(), getPassword(), absPath, type);
	}

	/**
	 * Get the path and query string e.g. /path/servlet?param1=true.
	 *
	 * @return the path and its query string
	 */
	public String getPathQuery() {
		final StringBuilder sb = new StringBuilder(BUFFER_SIZE);
		sb.append(getPath());
		sb.append("?");
		sb.append(getMount());

		return sb.toString();
	}

	/**
	 * Append query string to the uri.
	 *
	 * @return the uri
	 */
	@Override
	protected String createURI() {
		if (getMount() != null) {
			final StringBuilder sb = new StringBuilder(BUFFER_SIZE);
			sb.append(super.createURI());
			sb.append("?");
			sb.append(getMount());
			return sb.toString();
		}
		return super.createURI();
	}

	@Override
	protected void appendRootUri(StringBuilder buffer, boolean addPassword) {
		buffer.append(getScheme());
		buffer.append("://");
		appendCredentials(buffer, addPassword);
		buffer.append(hostName);
	}

	protected void appendCredentials(final StringBuilder buffer, final boolean addPassword) {
		if (userName != null && userName.length() != 0) {
			UriParser.appendEncoded(buffer, userName, USERNAME_RESERVED);
			if (password != null && password.length() != 0) {
				buffer.append(':');
				if (addPassword) {
					UriParser.appendEncoded(buffer, password, PASSWORD_RESERVED);
				} else {
					buffer.append("***");
				}
			}
			buffer.append('@');
		}
	}
}
