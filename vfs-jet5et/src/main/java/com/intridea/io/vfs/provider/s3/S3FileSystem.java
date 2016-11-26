package com.intridea.io.vfs.provider.s3;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.provider.UriParser;
import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3Bucket;

public class S3FileSystem extends AbstractFileSystem {
	private final static Log LOG = LogFactory.getLog(S3FileSystem.class);

	private S3Service service;
	private S3Bucket bucketObj;

	public S3FileSystem(S3FileName fileName, S3Service service, FileSystemOptions fileSystemOptions)
			throws FileSystemException {
		super(fileName, null, fileSystemOptions);

		String bucket = UriParser.extractFirstElement(new StringBuilder(fileName.getRoot().getPath()));
//		try {
			this.service = service;
			bucketObj = new S3Bucket(bucket);
//			if (!service.isBucketAccessible(bucket)) {
//				bucketObj = service.createBucket(bucket);
//			}
			LOG.info(String.format("Created Amazon 3S file system %s", bucket));
//		} catch (ServiceException e) {
//			String s3message = e.getMessage();
//			e.printStackTrace();
//			if (s3message != null) {
//				throw new FileSystemException(s3message);
//			} else {
//				throw new FileSystemException(e);
//			}
//		}
	}

	@Override
	protected void addCapabilities(Collection<Capability> capabilities) {
		capabilities.addAll(S3FileProvider.capabilities);
	}

	@Override
	protected FileObject createFile(AbstractFileName name) throws Exception {
		return new S3FileObject(name, this, service, bucketObj);
	}
}
