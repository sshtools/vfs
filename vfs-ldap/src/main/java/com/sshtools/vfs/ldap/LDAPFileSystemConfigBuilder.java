package com.sshtools.vfs.ldap;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.naming.Name;
import javax.net.SocketFactory;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

public class LDAPFileSystemConfigBuilder extends FileSystemConfigBuilder {
	private final static LDAPFileSystemConfigBuilder builder = new LDAPFileSystemConfigBuilder();
	private static final int DEFAULT_PAGE_SIZE = 1000;
	private static final String[] DEFAULT_FILE_OBJECT_CLASSES = { "organizationPerson", "user", "person", "group", "logonboxUser",
			"logonboxGroup" };
	private static final String[] DEFAULT_FOLDER_OBJECT_CLASSES = { "container", "logonboxContainer", "domain", "logonboxRealm",
			"organizationalUnit" };

	public static LDAPFileSystemConfigBuilder getInstance() {
		return builder;
	}

	private LDAPFileSystemConfigBuilder() {
	}

	public void setAuthentication(FileSystemOptions opts, String authType) {
		setParam(opts, "ldapAuth", authType);
	}

	public String getAuthentication(FileSystemOptions opts) {
		return (String) getParam(opts, "ldapAuth");
	}

	public void setSocketFactory(FileSystemOptions opts, SocketFactory socketFactory) {
		setParam(opts, "socketFactory", socketFactory);
	}

	public SocketFactory getSocketFactory(FileSystemOptions opts) {
		return (SocketFactory) getParam(opts, "socketFactory");
	}

	public void setMaxPageSize(FileSystemOptions opts, int maxPageSize) {
		setParam(opts, "maxPageSize", maxPageSize);
	}

	public int getMaxPageSize(FileSystemOptions opts) {
		Integer mps = (Integer) getParam(opts, "maxPageSize");
		return mps == null ? DEFAULT_PAGE_SIZE : mps;
	}

	public void setFollowReferrals(FileSystemOptions opts, boolean followReferals) {
		setParam(opts, "followReferrals", followReferals);
	}

	public boolean isFollowReferrals(FileSystemOptions opts) {
		Boolean fr = (Boolean) getParam(opts, "followReferrals");
		return fr == null ? false : fr;
	}

	@Override
	protected Class<? extends FileSystem> getConfigClass() {
		return LDAPFileSystem.class;
	}

	@SuppressWarnings("unchecked")
	public Collection<String> getFolderObjectClasses(FileSystemOptions opts) {
		Object incs = getParam(opts, "folderObjectClasses");
		return (Collection<String>) (incs == null ? Arrays.asList(DEFAULT_FOLDER_OBJECT_CLASSES) : incs);
	}

	@SuppressWarnings("unchecked")
	public Collection<String> getFileObjectClasses(FileSystemOptions opts) {
		Object incs = getParam(opts, "fileObjectClasses");
		return (Collection<String>) (incs == null ? Arrays.asList(DEFAULT_FILE_OBJECT_CLASSES) : incs);
	}

	public void setFileObjectClasses(FileSystemOptions opts, Collection<String> fileObjectClasses) {
		setParam(opts, "fileObjectClasses", fileObjectClasses);
	}

	public void setFolerObjectClasses(FileSystemOptions opts, Collection<String> folderObjectClasses) {
		setParam(opts, "folderObjectClasses", folderObjectClasses);
	}

	/**
	 * Get a list of distinguished names to exclude from the search. These are
	 * relative to the Base DN. If the list is empty, all paths should be
	 * included unless explicit excludes have been set.
	 * 
	 * @return paths to exclude
	 */
	@SuppressWarnings("unchecked")
	public Collection<Name> getIncludes(FileSystemOptions opts) {
		Object incs = getParam(opts, "includes");
		return (Collection<Name>) (incs == null ? Collections.emptyList() : incs);
	}

	/**
	 * Get a list of distinguished names to exclude from the search. These are
	 * relative to the Base DN. If the list is empty, all paths should be
	 * included unless explicit includes have been set.
	 * 
	 * @return paths to exclude
	 */
	@SuppressWarnings("unchecked")
	public Collection<Name> getExcludes(FileSystemOptions opts) {
		Object excs = getParam(opts, "excludes");
		return (Collection<Name>) (excs == null ? Collections.emptyList() : excs);
	}

	public void setExcludes(FileSystemOptions opts, Collection<Name> excludes) {
		setParam(opts, "excludes", excludes);
	}

	public void setIncludes(FileSystemOptions opts, Collection<Name> includes) {
		setParam(opts, "includes", includes);
	}
}
