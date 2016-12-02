package com.sshtools.vfs.nfs;

import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3;

public class NFSFileSystem extends AbstractFileSystem {


	private Nfs3 nfs3;

	public NFSFileSystem(NFSFileName fileName, FileSystemOptions fileSystemOptions, Nfs3 nfs3) throws FileSystemException {
		super(fileName, null, fileSystemOptions);
		this.nfs3 = nfs3;
	}

	@Override
	protected void addCapabilities(Collection<Capability> capabilities) {
		capabilities.addAll(NFSFileProvider.capabilities);
	}

	@Override
	protected FileObject createFile(AbstractFileName name) throws Exception {
		return new NFSFileObject(name, this, nfs3);
	}

}
