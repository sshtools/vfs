package com.sshtools.vfs.smbng;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

import jcifs.CIFSContext;
import jcifs.context.SingletonContext;

public class SmbFileSystemConfigBuilder extends FileSystemConfigBuilder {
	private static final String CONTEXT = "context";
	private final static SmbFileSystemConfigBuilder builder = new SmbFileSystemConfigBuilder();

	public static SmbFileSystemConfigBuilder getInstance() {
		return builder;
	}

	private SmbFileSystemConfigBuilder() {
	}

	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return SmbFileSystem.class;
	}

	public CIFSContext getContext(FileSystemOptions opts) {
		CIFSContext ctx = (CIFSContext) getParam(opts, CONTEXT);
		if (ctx == null) {
			ctx = SingletonContext.getInstance();
		}
		return ctx;
	}

	public void setContext(FileSystemOptions opts, CIFSContext context) {
		setParam(opts, CONTEXT, context);
	}
}
