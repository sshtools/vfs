package com.maverick.vfs.rfbftp;

import java.io.IOException;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.provider.GenericFileName;

import com.sshtools.rfb.RFBContext;

public class RFBFTPFileSystem extends AbstractFileSystem {
	final static Log log = LogFactory.getLog(RFBFTPFileSystem.class);
	private RFBFTPClient client;
	private String home;

	protected RFBFTPFileSystem(final GenericFileName rootName,
			final RFBFTPClient client, final FileSystemOptions fileSystemOptions) {
		super(rootName, null, fileSystemOptions);
		this.client = client;
	}

	protected void doCloseCommunicationLink() {
		try {
			if (client != null && !client.isShared()) {
				client.getDisplay().getEngine().getTransport().close();
			}
		} catch (Exception ex) {
			// #ifdef DEBUG
			log.debug("Failed to close communication link.", ex);
			// #endif
		} finally {
			client = null;
		}
	}

	public RFBFTPClient getClient() throws IOException {
		if (this.client == null) {
			try {
				final GenericFileName rootName = (GenericFileName) getRootName();
				RFBContext context = new RFBContext();
				client = RFBFTPClientFactory.createConnection(rootName.getHostName(), rootName.getPort(), rootName.getPassword(), getFileSystemOptions());				
			} catch (final Exception e) {
				throw new FileSystemException(
					"vfs.provider.sftp/connect.error", getRootName(), e);
			}
		}
		
		return client;
	}
	
	protected void addCapabilities(final Collection<Capability> caps) {
		caps.addAll(RFBFTPFileProvider.capabilities);
	}

	protected FileObject createFile(final AbstractFileName name)
			throws FileSystemException {
		return new RFBFTPFileObject(name, this);
	}

	public double getLastModTimeAccuracy() {
		return 1L;
	}

	public String getHome() {
		return home;
	}

}
