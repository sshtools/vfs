package com.sshtools.vfs.nfs;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

import com.emc.ecs.nfsclient.nfs.NfsSetAttributes;
import com.emc.ecs.nfsclient.nfs.NfsTime;
import com.emc.ecs.nfsclient.nfs.io.NfsFile;

public class NFSFileSystemConfigBuilder extends FileSystemConfigBuilder {
	private static final String AUTH = "auth";
	private static final String RETRIES = "retries";
	private static final String MOUNT = "mount";

	private static final String DEFAULT_FOLDER_PERMS = "defaultFolderPerms";
	
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

	public String getMount(FileSystemOptions opts) {
		return getString(opts, MOUNT, "/");
	}

	public void setMount(FileSystemOptions opts, String mount) {
		setParam(opts, MOUNT, mount);
	}

	public void setNewFolderPermissions(FileSystemOptions opts, NfsSetAttributes folderAttributes) {
		setParam(opts, DEFAULT_FOLDER_PERMS, folderAttributes);
	}
	
	public NfsSetAttributes getNewFolderPermissions(FileSystemOptions opts) {
		NfsSetAttributes attrs = (NfsSetAttributes) getParam(opts, DEFAULT_FOLDER_PERMS);
		if(attrs==null) {
			return new NfsSetAttributes(NfsFile.ownerReadModeBit | NfsFile.ownerWriteModeBit | NfsFile.ownerExecuteModeBit, 
					null, null,
					NfsTime.SET_TO_CURRENT_ON_SERVER,
					NfsTime.SET_TO_CURRENT_ON_SERVER);
		}
		return attrs;
	}
}
