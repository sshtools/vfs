package com.sshtools.vfs.webdav;

import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

import com.github.sardine.Sardine;

public class WebdavFileSystem extends AbstractFileSystem implements FileSystem {

	private Sardine sardine;

	protected WebdavFileSystem(FileName rootName, Sardine sardine, FileSystemOptions fileSystemOptions) {
		super(rootName, null /* there is no parent layer for a webdav file system */, fileSystemOptions);
		this.sardine = sardine;
	}

	@Override
	protected FileObject createFile(AbstractFileName name) throws Exception {
		return new WebdavFileObject(name, this, sardine);
	}

    /**
     * Adds the capabilities of this file system.
     */
    @Override
    protected void addCapabilities(final Collection<Capability> caps)
    {
        caps.addAll(WebdavFileProvider.capabilities);
    }
}
