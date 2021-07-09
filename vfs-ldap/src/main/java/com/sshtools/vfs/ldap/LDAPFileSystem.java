package com.sshtools.vfs.ldap;

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

public class LDAPFileSystem extends AbstractFileSystem {
	final static Log log = LogFactory.getLog(LDAPFileSystem.class);
	private LDAPClient client;
	private String home;

	protected LDAPFileSystem(final GenericFileName rootName, final FileSystemOptions fileSystemOptions) {
		super(rootName, null, fileSystemOptions);
	}

	protected void doCloseCommunicationLink() {
		try {
			if (client != null) {
				client.close();
			}
		} catch (Exception ex) {
			log.debug("Failed to close communication link.", ex);
		} finally {
			client = null;
		}
	}

	public LDAPClient getClient() throws IOException {
		if (this.client == null) {
			try {
				client = new DefaultLDAPClient((GenericFileName) getRootName(), getFileSystemOptions());
			} catch (final Exception e) {
				throw new FileSystemException("vfs.provider.ldap/connect.error", getRootName(), e);
			}
		}
		return client;
	}

	protected void addCapabilities(final Collection<Capability> caps) {
		caps.addAll(LDAPFileProvider.capabilities);
	}

	protected FileObject createFile(final AbstractFileName name) throws FileSystemException {
		if (name.getBaseName().equals("")) {
			return new RootDSEFileObject(name, this);
		}
		return new LDAPFileObject(name, this);
	}

	public double getLastModTimeAccuracy() {
		return 1L;
	}

	public String getHome() {
		return home;
	}
}
