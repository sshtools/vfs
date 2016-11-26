package com.sshtools.vfs.s3.provider.s3.acl;

import java.util.Collection;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.operations.FileOperation;
import org.apache.commons.vfs2.operations.FileOperationProvider;

import com.sshtools.vfs.s3.operations.acl.IAclGetter;
import com.sshtools.vfs.s3.operations.acl.IAclSetter;
import com.sshtools.vfs.s3.provider.s3.S3FileObject;

public class AclOperationsProvider implements FileOperationProvider {
	@Override
	public void collectOperations(Collection<Class<? extends FileOperation>> operationsList, FileObject file)
			throws FileSystemException {
		if (file instanceof S3FileObject) {
			operationsList.add(AclGetter.class);
			operationsList.add(AclSetter.class);
		}
	}

	@Override
	public FileOperation getOperation(FileObject file, Class<? extends FileOperation> operationClass) throws FileSystemException {
		if (file instanceof S3FileObject) {
			if (operationClass.equals(IAclGetter.class)) {
				// getter
				return new AclGetter((S3FileObject) file);
			} else if (operationClass.equals(IAclSetter.class)) {
				// setter
				return new AclSetter((S3FileObject) file);
			}
		}
		throw new FileSystemException(
				String.format("Operation %s is not provided for file %s", operationClass.getName(), file.getName()));
	}
}
