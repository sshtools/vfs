package com.sshtools.vfs.googledrive;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.GenericFileName;

public class GDriveFileName extends GenericFileName {

	protected GDriveFileName(String scheme, String path, FileType type) {
		super(scheme, null, 0, 0, null, null, path, type);
	}

	@Override
	public FileName createName(String absPath, FileType type) {
		return new GDriveFileName(getScheme(), absPath, type);
	}

	@Override
	protected void appendRootUri(StringBuilder buffer, boolean addPassword) {
		buffer.append(getScheme());
		buffer.append("://");
	}
}
