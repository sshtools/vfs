package com.sshtools.vfs.nfs;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

public class NFSFileSystemConfigBuilder extends FileSystemConfigBuilder {
	private static final String MODE = "mode";
	private final static NFSFileSystemConfigBuilder builder = new NFSFileSystemConfigBuilder();

	public enum Mode {
		ATTACH_DETACH, LIVE
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

	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return NFSFileSystem.class;
	}
}
