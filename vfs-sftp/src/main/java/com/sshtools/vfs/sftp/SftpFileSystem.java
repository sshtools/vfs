package com.sshtools.vfs.sftp;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.provider.GenericFileName;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.common.logger.Log;
import com.sshtools.common.ssh.SshConnection;

public class SftpFileSystem extends AbstractFileSystem {

	private SshConnection ssh;
	private SftpClient sftp;

	private String home;

	protected SftpFileSystem(final GenericFileName rootName, final SshConnection ssh, final FileSystemOptions fileSystemOptions) {
		super(rootName, null, fileSystemOptions);
		this.ssh = ssh;
	}

	protected void doCloseCommunicationLink() {
		try {
			if (sftp != null) {
				sftp.quit();
				sftp = null;
			}
		} catch (Exception ex) {
			if(Log.isDebugEnabled()) {
				Log.debug("Failed to close communication link.", ex);
			}
		}
	}

	protected synchronized SftpClient getClient() throws IOException {
		try {
			/*
			 * We always maintain at least one sftp client all the while the
			 * connection is opened. If the client is in use (i.e. it hasn't be
			 * release by calling the putClient() method), then a new one is
			 * created.
			 */
			final SftpClient sftp;
			if (this.sftp != null && !this.sftp.isClosed()) {
				sftp = this.sftp;
				this.sftp = null;
			} else {
				sftp = new SftpClient(ssh);
				home = sftp.pwd();
			}
			return sftp;
		} catch (final Exception e) {
			throw new FileSystemException("vfs.provider.sftp/connect.error", getRootName(), e);
		}
	}
	
	protected SshConnection getSsh() {
		return ssh;
	}

	protected void putClient(final SftpClient sftp) {
		if (this.sftp == null) {
			this.sftp = sftp;
		} else {
			try {
				sftp.quit();
			} catch (Exception e) {
			}
		}
	}

	protected void addCapabilities(final Collection<Capability> caps) {
		caps.addAll(SftpFileProvider.capabilities);
	}

	protected FileObject createFile(final AbstractFileName name) throws FileSystemException {
		return new SftpFileObject(name, this);
	}

	public double getLastModTimeAccuracy() {
		return 1000L;
	}

	public String getHome() {
		return home;
	}
}
