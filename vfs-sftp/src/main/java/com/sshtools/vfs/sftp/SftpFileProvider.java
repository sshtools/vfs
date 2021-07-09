package com.sshtools.vfs.sftp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;
import org.apache.commons.vfs2.provider.GenericFileName;

import com.sshtools.common.ssh.SshConnection;

public class SftpFileProvider extends AbstractOriginatingFileProvider {

	protected final static Collection<Capability> capabilities = Collections.unmodifiableCollection(Arrays.asList(new Capability[] {
		Capability.CREATE, Capability.DELETE, Capability.RENAME, Capability.GET_TYPE, Capability.LIST_CHILDREN,
		Capability.READ_CONTENT, Capability.URI, Capability.WRITE_CONTENT, Capability.GET_LAST_MODIFIED,
		Capability.SET_LAST_MODIFIED_FILE, Capability.RANDOM_ACCESS_READ }));

	public SftpFileProvider() {
		super();
		setFileNameParser(SftpFileNameParser.getInstance());
	}

	protected FileSystem doCreateFileSystem(final FileName name, final FileSystemOptions fileSystemOptions)
			throws FileSystemException {

		// Create the file system
		final GenericFileName rootName = (GenericFileName) name;

		SshConnection ssh;
		try {
			ssh = SftpClientFactory.createConnection(rootName.getHostName(), rootName.getPort(), rootName.getUserName(),
				rootName.getPassword(), fileSystemOptions);
		} catch (final Exception e) {
			throw new FileSystemException("vfs.provider.sftp/connect.error", name, e);
		}

		return new SftpFileSystem(rootName, ssh, fileSystemOptions);
	}

	public void init() throws FileSystemException {
	}

	public FileSystemConfigBuilder getConfigBuilder() {
		return SftpFileSystemConfigBuilder.getInstance();
	}

	public Collection<Capability> getCapabilities() {
		return capabilities;
	}
}
