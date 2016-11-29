package com.sshtools.vfs.googledrive;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.GenericFileName;
import org.apache.commons.vfs2.provider.UriParser;

public class GDriveFileName extends GenericFileName {

	protected GDriveFileName(final String path, final FileType type) {
		this(null, null, path, type);
	}

	protected GDriveFileName(String userName, String password, final String path, final FileType type) {
		super("gdrive", "google.com", 443, 443, userName, password, path, type);
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
		return new GDriveFileName(getUserName(), getPassword(), absPath, type);
	}

}
