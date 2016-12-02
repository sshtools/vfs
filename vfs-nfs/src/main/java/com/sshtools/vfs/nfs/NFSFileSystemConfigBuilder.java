package com.sshtools.vfs.nfs;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

public class NFSFileSystemConfigBuilder extends FileSystemConfigBuilder {
	private static final String MODE = "mode";
	private static final String AUTH = "auth";
	private static final String RETRIES = "retries";
	private final static NFSFileSystemConfigBuilder builder = new NFSFileSystemConfigBuilder();

	public enum Mode {
		ATTACH_DETACH, LIVE
	}

	public enum Auth {
		UNIX, NONE
	}

	public static NFSFileSystemConfigBuilder getInstance() {
		return builder;
	}

	private NFSFileSystemConfigBuilder() {
	}

	public void setMode(FileSystemOptions opts, Mode mode) {
		setParam(opts, MODE, mode);
	}

	public Mode getMode(FileSystemOptions opts) {
		Mode m = (Mode) getParam(opts, MODE);
		return m == null ? Mode.ATTACH_DETACH : m;
	}

	public void setAuth(FileSystemOptions opts, Auth mode) {
		setParam(opts, AUTH, mode);
	}

	public Auth getAuth(FileSystemOptions opts) {
		Auth m = (Auth) getParam(opts, AUTH);
		return m == null ? Auth.UNIX : m;
	}

	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return NFSFileSystem.class;
	}

	public int getRetries(FileSystemOptions opts) {
		return getInteger(opts, RETRIES, 3);
	}

	public void setRetries(FileSystemOptions opts, int retries) {
		setParam(opts, RETRIES, retries);
	}
}
