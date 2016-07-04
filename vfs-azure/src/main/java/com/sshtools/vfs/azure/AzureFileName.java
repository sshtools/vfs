package com.sshtools.vfs.azure;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.GenericFileName;
import org.apache.commons.vfs2.provider.UriParser;

public class AzureFileName extends GenericFileName {

	protected AzureFileName(final String path, final FileType type) {
		this(null, null, path, type);
	}

	protected AzureFileName(String userName, String password, final String path, final FileType type) {
		super("azure", "azure", 443, 443, userName, password, path, type);
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
		return new AzureFileName(getUserName(), getPassword(), absPath, type);
	}

}
