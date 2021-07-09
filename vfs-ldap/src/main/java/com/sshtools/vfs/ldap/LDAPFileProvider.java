package com.sshtools.vfs.ldap;

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

public class LDAPFileProvider extends AbstractOriginatingFileProvider {
	protected final static Collection<Capability> capabilities = Collections.unmodifiableCollection(
			Arrays.asList(new Capability[] { Capability.CREATE, Capability.DELETE, Capability.RENAME, Capability.GET_TYPE,
					Capability.LIST_CHILDREN, Capability.READ_CONTENT, Capability.URI, Capability.WRITE_CONTENT,
					Capability.ATTRIBUTES }));

	public LDAPFileProvider() {
		super();
		setFileNameParser(LDAPFileNameParser.getInstance());
	}

	protected FileSystem doCreateFileSystem(final FileName name, final FileSystemOptions fileSystemOptions)
			throws FileSystemException {
		// Create the file system
		final GenericFileName rootName = (GenericFileName) name;
		return new LDAPFileSystem(rootName, fileSystemOptions);
	}

	public void init() throws FileSystemException {
	}

	public FileSystemConfigBuilder getConfigBuilder() {
		return LDAPFileSystemConfigBuilder.getInstance();
	}

	@Override
	public Collection<Capability> getCapabilities() {
		return capabilities;
	}
}
