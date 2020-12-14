package com.sshtools.vfs.azure;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;

public class AzureFileName extends AbstractFileName {

	protected AzureFileName(final String scheme, final String path, final FileType type) {
		super(scheme, path, type);
	}

	@Override
	public FileName createName(String absPath, FileType type) {
		return new AzureFileName(getScheme(), absPath, type);
	}

	@Override
	protected void appendRootUri(StringBuilder buffer, boolean addPassword) {
		buffer.append(getScheme());
		buffer.append("://");
	}
}
