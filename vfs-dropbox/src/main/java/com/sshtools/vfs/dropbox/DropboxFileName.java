package com.sshtools.vfs.dropbox;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;

public class DropboxFileName extends AbstractFileName {
	public DropboxFileName(String scheme, String absPath, FileType type) {
		super(scheme, absPath, type);
	}

	@Override
	public FileName createName(String absPath, FileType type) {
		return new DropboxFileName(getScheme(), absPath, type);
	}

	@Override
	protected void appendRootUri(StringBuilder buffer, boolean addPassword) {
		buffer.append(getScheme());
		buffer.append("://");
	}
}
