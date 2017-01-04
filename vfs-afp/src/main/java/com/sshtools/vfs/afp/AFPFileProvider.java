package com.sshtools.vfs.afp;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.jmdns.JmDNS;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;

public class AFPFileProvider extends AbstractOriginatingFileProvider {
	final static Log LOG = LogFactory.getLog(AFPFileProvider.class);
	public final static Collection<Capability> capabilities = Collections
			.unmodifiableCollection(Arrays.asList(Capability.CREATE, Capability.DELETE,
					// Capability.RENAME,
					Capability.ATTRIBUTES, Capability.GET_TYPE, Capability.GET_LAST_MODIFIED, Capability.LIST_CHILDREN,
					Capability.READ_CONTENT, Capability.URI, Capability.WRITE_CONTENT));
	public final static UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = new UserAuthenticationData.Type[] {
			UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD };
	/** Global instance of the JSON factory. */
	private static FileSystemOptions defaultOptions = new FileSystemOptions();

	public static FileSystemOptions getDefaultFileSystemOptions() {
		return defaultOptions;
	}

	private static JmDNS jmDns;
	private static int jmDnsAcquired = 0;
	private static Object jmDnsLock = new Object();

	public AFPFileProvider() {
		super();
		setFileNameParser(new AFPFileNameParser());
	}

	static void releaseJmDns() {
		synchronized (jmDnsLock) {
			jmDnsAcquired--;
			if (jmDnsAcquired <= 0)
				try {
					jmDns.close();
				} catch (IOException e) {
				}
			jmDns = null;
		}
	}

	static JmDNS getJmDns() {
		synchronized (jmDnsLock) {
			if (jmDns == null) {
				try {
					jmDns = JmDNS.create();
				} catch (IOException e) {
					LOG.error("Failed to create JmDNS.", e);
				}
			}
			jmDnsAcquired++;
			return jmDns;
		}
	}

	protected FileSystem doCreateFileSystem(FileName fileName, FileSystemOptions fileSystemOptions) throws FileSystemException {
		final FileSystemOptions fsOptions = fileSystemOptions != null ? fileSystemOptions : getDefaultFileSystemOptions();
		try {
			final AFPFileName fn = (AFPFileName) fileName;
			return new AFPFileSystem(fn, fsOptions);
		} catch (IOException e) {
			throw new FileSystemException(e.getMessage(), e);
		}
	}

	public Collection<Capability> getCapabilities() {
		return capabilities;
	}
}
