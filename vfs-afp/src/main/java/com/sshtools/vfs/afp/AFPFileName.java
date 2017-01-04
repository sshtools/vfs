package com.sshtools.vfs.afp;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.GenericFileName;

public class AFPFileName extends GenericFileName {
	public static final int DEFAULT_PORT = 548;
	private final String volume;

	protected AFPFileName(AFPFileName parent, final String path, final FileType type) {
		this(parent.getScheme(), parent.getHostName(), parent.getPort(), parent.getUserName(), parent.getPassword(),
				parent.getVolume(), path, type);
	}

	protected AFPFileName(final String scheme, final String hostName, final int port, final String userName, final String password,
			final String volume, final String path, final FileType type) {
		super(scheme, hostName, port, DEFAULT_PORT, userName, password, path, type);
		this.volume = volume;
	}

	public String getVolume() {
		return volume;
	}

	/**
	 * Returns the base name of the file.
	 * 
	 * @return The base name of the file.
	 */
	@Override
	public String getBaseName() {
		String bn = super.getBaseName();
		if (bn.equals("")) {
			bn = getVolume();
			if (bn == null) {
				bn = getHostName();
				if (getPort() != getDefaultPort()) {
					bn += ":" + getPort();
				}
			}
		}
		return bn;
	}

	@Override
	public FileName getParent() {
		FileName par = super.getParent();
		if (par == null && getScheme() != null) {
			if (getVolume() != null)
				return new AFPFileName(getScheme(), getHostName(), getPort(), getUserName(), getPassword(), null, null,
						FileType.FOLDER);
			else if (getHostName() != null)
				return new AFPFileName(getScheme(), null, -1, getUserName(), getPassword(), null, null, FileType.FOLDER);
		}
		return par;
	}

	protected String createURI() {
		return createUri(true);
	}

	private String createUri(boolean addPassword) {
		final StringBuilder buffer = new StringBuilder();
		appendRootUri(buffer, addPassword);
		if (getHostName() != null) {
			buffer.append(getHostName());
			if (getPort() != getDefaultPort()) {
				buffer.append(":");
				buffer.append(getPort());
			}
		}
		if (getVolume() != null) {
			buffer.append(SEPARATOR_CHAR);
			buffer.append(getVolume());
			buffer.append(getPath());
		}
		return buffer.toString();
	}

	@Override
	protected void appendRootUri(final StringBuilder buffer, final boolean addPassword) {
		/*
		 * Because this file system is valid when any or all of hostname and
		 * share is ommited, we can only have the scheme and userinfo ine root
		 * URI or the check for root URI of the file system will fail for
		 * descendents.
		 * 
		 * To compliment this, we must also override createURI() and
		 * getFriendURI() to append the hostname and port so the final URI is
		 * correct
		 * 
		 * Commons VFS does not seem to really like having this kind of URI
		 * arrangement, and is presumably why SMB does not have the same kind of
		 * 'browsing' facility even though jCIFS supports it.
		 */
		buffer.append(getScheme());
		buffer.append("://");
		appendCredentials(buffer, addPassword);
	}

	@Override
	public FileName createName(String path, final FileType type) {
		return new AFPFileName(getScheme(), getHostName(), getPort(), getUserName(), getPassword(), volume, path, type);
	}

	@Override
	public String getFriendlyURI() {
		return createUri(false);
	}
}
