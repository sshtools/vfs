package com.sshtools.vfs.ext;

import org.apache.commons.vfs2.FileObject;

public interface FileObjectEventListener {
	void fireDeletedFile(FileObject f);

	void fireNewFile(FileObject f);

	void fireUpdatedFile(FileObject f);
}