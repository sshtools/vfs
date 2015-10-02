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

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;

/**
 * An S3 file system.
 * 
 * @author Marat Komarov
 * @author Matthias L. Jugel
 */
public class S3FileSystem extends AbstractFileSystem {
	private AmazonS3Client service;
	private Bucket bucket;
	private Log logger = LogFactory.getLog(S3FileSystem.class);

	public S3FileSystem(S3FileName fileName, AmazonS3Client service,
			FileSystemOptions fileSystemOptions) throws FileSystemException {
		super(fileName, null, fileSystemOptions);

		String bucketId = UriParser.extractFirstElement(new StringBuilder(fileName.getRoot().getPath()));
		try {
			this.service = service;
			bucket = new Bucket(bucketId);
			if (!service.doesBucketExist(bucketId)) {
				bucket = service.createBucket(bucketId);
			}
			logger.info(String.format("Created new S3 FileSystem %s", bucketId));
		} catch (AmazonServiceException e) {
			String s3message = e.getMessage();
			if (s3message != null) {
				throw new FileSystemException(s3message);
			} else {
				throw new FileSystemException(e);
			}
		}
	}

	@Override
	protected FileObject createFile(AbstractFileName name) throws Exception {
		return new S3FileObject(name, this, service, bucket);
	}

	@Override
	protected void addCapabilities(Collection<Capability> caps) {
		caps.addAll(S3FileProvider.capabilities);
	}
}
