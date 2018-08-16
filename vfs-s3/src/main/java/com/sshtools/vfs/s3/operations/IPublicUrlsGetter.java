package com.sshtools.vfs.s3.operations;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.operations.FileOperation;

/**
 * File operation for gettin' direct urls to S3 objects.
 *
 * @author <A href="mailto:alexey@abashev.ru">Alexey Abashev</A>
 * @version $Id$
 */
public interface IPublicUrlsGetter extends FileOperation {
    /**
     * Get direct http url to file.
     *
     * @return HTTP url
     */
    String getHttpUrl();

    /**
     * Get private url in format s3://awsKey:awsSecretKey/bucket-name/object-name
     *
     * @return private URL
     */
    String getPrivateUrl();

    /**
     * Get signed URL.
     * 
     * @param expireInSeconds expire in seconds
     * @return signed URL
     * @throws FileSystemException on error
     */
    String getSignedUrl(int expireInSeconds) throws FileSystemException;
}
