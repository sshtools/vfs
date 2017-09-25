package com.sshtools.vfs.gcs;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;

public class GoogleStorageFileProvider extends AbstractOriginatingFileProvider {

	public final static Collection<Capability> capabilities = Collections.unmodifiableCollection(Arrays.asList(
	        Capability.CREATE,
	        Capability.DELETE,
	        Capability.GET_TYPE,
	        Capability.GET_LAST_MODIFIED,
	        Capability.SET_LAST_MODIFIED_FILE,
	        Capability.SET_LAST_MODIFIED_FOLDER,
	        Capability.LIST_CHILDREN,
	        Capability.READ_CONTENT,
	        Capability.URI,
	        Capability.WRITE_CONTENT
	    ));
	
	public Collection<Capability> getCapabilities() {
		return capabilities;
	}

	@Override
	protected FileSystem doCreateFileSystem(FileName rootName, FileSystemOptions fileSystemOptions)
			throws FileSystemException {
		return new GoogleStorageFileSystem(rootName, null, fileSystemOptions);
	}

}
