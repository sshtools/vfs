package com.sshtools.vfs.s3.provider.s3.operations;

import org.apache.commons.vfs2.FileSystemException;

import com.sshtools.vfs.s3.operations.IMD5HashGetter;
import com.sshtools.vfs.s3.provider.s3.S3FileObject;

/**
 * @author <A href="mailto:alexey@abashev.ru">Alexey Abashev</A>
 * @version $Id$
 */
public class MD5HashGetter implements IMD5HashGetter {
    private final S3FileObject file;

    /**
     * Constructor.
     * 
     * @param file file
     */
    public MD5HashGetter(S3FileObject file) {
        this.file = file;
    }

    /* (non-Javadoc)
     * @see com.sshtools.vfs.s3.operations.IMD5HashGetter#getMD5Hash()
     */
    @Override
    public String getMD5Hash() throws FileSystemException {
        return file.getMD5Hash();
    }

    /* (non-Javadoc)
     * @see org.apache.commons.vfs.operations.FileOperation#process()
     */
    @Override
    public void process() throws FileSystemException {

        // Do nothing
    }
}
