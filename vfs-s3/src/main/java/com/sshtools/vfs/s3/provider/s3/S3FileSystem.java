package com.sshtools.vfs.s3.provider.s3;

import java.io.FileNotFoundException;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.util.StringUtils;

/**
 * An S3 file system.
 *
 * @author Marat Komarov
 * @author Matthias L. Jugel
 * @author Moritz Siuts
 */
public class S3FileSystem extends AbstractFileSystem {

    private static final Log logger = LogFactory.getLog(S3FileSystem.class);

    private final AmazonS3 service;
    private Bucket bucket = null;

    private boolean shutdownServiceOnClose = false;

    public S3FileSystem(
            S3FileName fileName, AmazonS3 service, FileSystemOptions fileSystemOptions
    ) throws FileSystemException {
        super(fileName, null, fileSystemOptions);

        String bucketId = fileName.getBucketId();

        this.service = service;

        try {
        		if(!StringUtils.isNullOrEmpty(bucketId)) {
	            if (service.doesBucketExistV2(bucketId)) {
	                bucket = new Bucket(bucketId);
	            } else {
	                bucket = service.createBucket(bucketId);
	                logger.debug("Created new bucket.");
	            }
        		} else {
        			throw new FileSystemException(new FileNotFoundException("A bucket name is required."));
        		}
            logger.info("Created new S3 FileSystem " + bucketId);
        } catch (AmazonServiceException e) {
            String s3message = e.getMessage();

            if (s3message != null) {
                throw new FileSystemException(s3message, e);
            } else {
                throw new FileSystemException(e);
            }
        }
    }

    @Override
    protected void addCapabilities(Collection<Capability> caps) {
        caps.addAll(S3FileProvider.capabilities);
    }

    protected Bucket getBucket() {
        return bucket;
    }

    protected Regions getRegion() {
        return S3FileSystemConfigBuilder.getInstance().getRegion(getFileSystemOptions());
    }

    protected AmazonS3 getService() {
        return service;
    }

    @Override
    protected FileObject createFile(AbstractFileName fileName) throws Exception {
        return new S3FileObject(fileName, this);
    }

    @Override
    protected void doCloseCommunicationLink() {
        if (shutdownServiceOnClose) {
            service.shutdown();
        }
    }

    public void setShutdownServiceOnClose(boolean shutdownServiceOnClose) {
        this.shutdownServiceOnClose = shutdownServiceOnClose;
    }
}
