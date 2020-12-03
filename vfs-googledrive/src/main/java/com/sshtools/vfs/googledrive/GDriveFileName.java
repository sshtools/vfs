package com.sshtools.vfs.googledrive;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.UriParser;

public class GDriveFileName extends AbstractFileName {

	protected GDriveFileName(String scheme, String path, FileType type) {
		super(scheme, path, type);
	}

	public String getContainer() {
		StringBuilder bui = new StringBuilder(getPath());
		return UriParser.extractFirstElement(bui);
	}

	public String getPathAfterContainer() {
		StringBuilder bui = new StringBuilder(getPath());
		UriParser.extractFirstElement(bui);
		return bui.toString();
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
