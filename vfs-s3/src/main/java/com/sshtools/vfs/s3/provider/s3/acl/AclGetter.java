package com.sshtools.vfs.s3.provider.s3.acl;

import org.apache.commons.vfs2.FileSystemException;

import com.sshtools.vfs.s3.operations.acl.Acl;
import com.sshtools.vfs.s3.operations.acl.IAclGetter;
import com.sshtools.vfs.s3.operations.acl.Acl.Group;
import com.sshtools.vfs.s3.provider.s3.S3FileObject;

public class AclGetter implements IAclGetter {

	private S3FileObject file;
	
	private Acl acl;
	
	public AclGetter (S3FileObject file) {
		this.file = file;
	}
	
	public boolean canRead(Group group) {
		return acl.isAllowed(group, Acl.Permission.READ);
	}

	public boolean canWrite(Group group) {
		return acl.isAllowed(group, Acl.Permission.WRITE);
	}

	public Acl getAcl() {
		return acl;
	}

	public void process() throws FileSystemException {
		acl = file.getAcl();
	}

}
