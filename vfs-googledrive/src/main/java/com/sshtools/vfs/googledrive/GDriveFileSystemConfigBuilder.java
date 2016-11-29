package com.sshtools.vfs.googledrive;

import java.net.URL;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

public class GDriveFileSystemConfigBuilder extends FileSystemConfigBuilder {
	private static final String MODE = "mode";
	private static final String JSON_CLIENT_ID_RESOURCE = "jsonClientIdResource";
	private final static GDriveFileSystemConfigBuilder builder = new GDriveFileSystemConfigBuilder();

	public enum Mode {
		OAUTH, SERVICE
	}

	public static GDriveFileSystemConfigBuilder getInstance() {
		return builder;
	}

	private GDriveFileSystemConfigBuilder() {
	}

	public void setMode(FileSystemOptions opts, Mode mode) {
		setParam(opts, MODE, mode);
	}

	public Mode getMode(FileSystemOptions opts) {
		Mode m = (Mode) getParam(opts, MODE);
		return m == null ? Mode.OAUTH : m;
	}

	public void setClientIdJSON(FileSystemOptions opts, URL jsonClientIdResource) {
		setParam(opts, JSON_CLIENT_ID_RESOURCE, jsonClientIdResource);
	}

	public URL getClientIdJSON(FileSystemOptions opts) {
		return (URL) getParam(opts, JSON_CLIENT_ID_RESOURCE);
	}

	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return GDriveFileSystem.class;
	}
}
