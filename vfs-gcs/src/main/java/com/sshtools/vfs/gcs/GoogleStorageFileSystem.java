package com.sshtools.vfs.gcs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

public class GoogleStorageFileSystem extends AbstractFileSystem {

	Storage storage = null; 
	
	protected GoogleStorageFileSystem(FileName rootName, FileObject parentLayer, FileSystemOptions fileSystemOptions) {
		super(rootName, parentLayer, fileSystemOptions);
	}

	@Override
	protected FileObject createFile(AbstractFileName name) throws Exception {
		return new GoogleFileObject(name, this);
	}

	@Override
	protected void addCapabilities(Collection<Capability> caps) {
		caps.addAll(GoogleStorageFileProvider.capabilities);
	}
	
	Storage setupStorage() throws IOException {
		if(storage!=null) {
			return storage;
		}
        ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(new ByteArrayInputStream(
				GoogleStorageFileSystemConfigBuilder.getInstance().getClientIdJSON(getFileSystemOptions()).getBytes("UTF-8")));
        StorageOptions.Builder optionsBuilder = StorageOptions.newBuilder();
        optionsBuilder.setCredentials(credentials);
        return storage = optionsBuilder.build().getService();
    }
	
	String getBucketName(FileName name) {

		String path = name.getPath();
		int idx = path.indexOf('/', 1);
		if(idx > -1) {
			return name.getPath().substring(1, idx);
		} else {
			return name.getPath().substring(1);
		}
	}
	
	String getBucketPath(FileName name) {
		int idx = name.getPath().indexOf('/',1);
		if(idx > -1) {
			return name.getPath().substring(idx+1);
		} else {
			return "";
		}
	}



}
