package com.sshtools.vfs.gcs;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;

public class GoogleFileName extends AbstractFileName {

	public GoogleFileName(String absPath, FileType type) {
		super("gcs", absPath, type);
	}

	@Override
	public FileName createName(String absPath, FileType type) {
		return new GoogleFileName(absPath, type);
	}

	@Override
	protected void appendRootUri(StringBuilder buffer, boolean addPassword) {
		buffer.append(getScheme());
        buffer.append("://");
	}

}
