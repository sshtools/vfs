package com.sshtools.vfs.gcs;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

public class GoogleStorageFileSystemConfigBuilder extends FileSystemConfigBuilder {

	private static final String JSON_CLIENT_ID_RESOURCE = "jsonClientIdResource";
	private final static GoogleStorageFileSystemConfigBuilder builder = new GoogleStorageFileSystemConfigBuilder();

	public static GoogleStorageFileSystemConfigBuilder getInstance() {
		return builder;
	}

	private GoogleStorageFileSystemConfigBuilder() {
	}

	public void setClientIdJSON(FileSystemOptions opts, String jsonClientIdResource) {
		setParam(opts, JSON_CLIENT_ID_RESOURCE, jsonClientIdResource);
	}

	public String getClientIdJSON(FileSystemOptions opts) {
		return (String) getParam(opts, JSON_CLIENT_ID_RESOURCE);
	}

	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return GoogleStorageFileSystem.class;
	}
}
