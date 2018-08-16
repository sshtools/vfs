package com.sshtools.vfs.s3.operations;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.operations.FileOperation;

/**
 * Interface for getting file Access Control List.
 *
 * @author Marat Komarov
 */
public interface IAclGetter extends FileOperation {

    /**
     * Returns true when file is readable
     * @param group group
     * @return can read
     */
    boolean canRead(Acl.Group group);

    /**
     * Returns true when file is writeable
     * @param group group
     * @return can write
     */
    boolean canWrite(Acl.Group group);

    /**
     * Returns file ACL
     * @return acl
     */
    Acl getAcl();

    /**
     * Executes getter operation.
     * Must be called before any other operation methods
     * 
     * @throws FileSystemException on error
     */
    @Override
    void process() throws FileSystemException;
}
