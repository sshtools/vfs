package com.sshtools.vfs.s3.operations;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.operations.FileOperation;

/**
 * Get md5 hash for file.
 *
 * @author <A href="mailto:alexey@abashev.ru">Alexey Abashev</A>
 * @version $Id$
 */
public interface IMD5HashGetter extends FileOperation {
    /**
     * Get MD5 hash for object.
     *
     * @return hash
     * @throws FileSystemException on error
     */
    String getMD5Hash() throws FileSystemException;
}
