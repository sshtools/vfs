package com.sshtools.vfs.afp;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

import com.sshtools.jafp.common.AFPConstants;

public class AFPFileSystemConfigBuilder extends FileSystemConfigBuilder {
	private static final String VOLUME = "volume";
	private static final String AUTHENTICATION_METHODS = "authenticationMethods";
	private final static AFPFileSystemConfigBuilder builder = new AFPFileSystemConfigBuilder();

	public enum Auth {
		CLEAR_TEXT, NONE
	}

	public static AFPFileSystemConfigBuilder getInstance() {
		return builder;
	}

	private AFPFileSystemConfigBuilder() {
	}

	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return AFPFileSystem.class;
	}

	public String getVolume(FileSystemOptions opts) {
		return getString(opts, VOLUME, "/");
	}

	public void setMount(FileSystemOptions opts, String mount) {
		setParam(opts, VOLUME, mount);
	}

	public List<String> getAuthenticationMethods(FileSystemOptions opts) {
		@SuppressWarnings("unchecked")
		List<String> opt = (List<String>) getParam(opts, AUTHENTICATION_METHODS);
		if (opt == null)
			opt = Arrays.asList(AFPConstants.UAM_STR_GUEST, AFPConstants.UAM_STR_DHX_128,
					AFPConstants.UAM_STR_CLEARTEXT);
		return opt;
	}

	public void setAuthenticationMethods(FileSystemOptions opts, String... authenticationMethods) {
		setAuthenticationMethods(opts, Arrays.asList(authenticationMethods));
	}

	public void setAuthenticationMethods(FileSystemOptions opts, List<String> authenticationMethods) {
		setParam(opts, AUTHENTICATION_METHODS, authenticationMethods);
	}
}
