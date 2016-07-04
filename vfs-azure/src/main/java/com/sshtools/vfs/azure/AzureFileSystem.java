package com.sshtools.vfs.azure;

import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

import com.microsoft.azure.storage.blob.CloudBlobClient;

public class AzureFileSystem extends AbstractFileSystem {

	private CloudBlobClient client;

	public AzureFileSystem(AzureFileName fileName, CloudBlobClient service, FileSystemOptions fileSystemOptions)
			throws FileSystemException {
		super(fileName, null, fileSystemOptions);
		this.client = service;
	}

	@Override
	protected void addCapabilities(Collection<Capability> capabilities) {
		capabilities.addAll(AzureFileProvider.capabilities);
	}

	@Override
	protected FileObject createFile(AbstractFileName name) throws Exception {
		return new AzureFileObject(name, this, client);
	}
}
