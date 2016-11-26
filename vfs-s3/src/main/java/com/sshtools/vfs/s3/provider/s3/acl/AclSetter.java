package com.sshtools.vfs.s3.provider.s3.acl;

import org.apache.commons.vfs2.FileSystemException;

import com.sshtools.vfs.s3.operations.acl.Acl;
import com.sshtools.vfs.s3.operations.acl.IAclSetter;
import com.sshtools.vfs.s3.provider.s3.S3FileObject;

public class AclSetter implements IAclSetter {

	private S3FileObject file;

	private Acl acl;

	public AclSetter(S3FileObject file) {
		this.file = file;
	}

	public void setAcl(Acl acl) {
		this.acl = acl;
	}

	public void process() throws FileSystemException {
		file.setAcl(acl);
	}
}
