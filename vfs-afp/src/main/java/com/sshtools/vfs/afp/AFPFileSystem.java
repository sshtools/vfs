package com.sshtools.vfs.afp;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jmdns.JmDNS;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

import com.sshtools.afp.client.AFPAuthenticator;
import com.sshtools.afp.client.AFPClient;

public class AFPFileSystem extends AbstractFileSystem {

	private List<WeakReference<AFPClient>> clients = new ArrayList<WeakReference<AFPClient>>();
	private JmDNS jmDNS;

	public AFPFileSystem(AFPFileName fileName, FileSystemOptions fileSystemOptions) throws FileSystemException {
		super(fileName, null, fileSystemOptions);
	}

	public JmDNS getJmDns() {
		if (jmDNS == null)
			jmDNS = AFPFileProvider.getJmDns();
		return jmDNS;
	}

	@Override
	public void close() {
		if (jmDNS != null)
			AFPFileProvider.releaseJmDns();
		for (WeakReference<AFPClient> c : clients) {
			AFPClient cl = c.get();
			if (cl != null) {
				try {
					cl.close();
				} catch (IOException e) {
				}
			}
		}
		super.close();
	}

	AFPClient getClient(final String hostname, final String defUsername, final int port,
			final FileSystemOptions fsOptions) {
		AFPClient client = new AFPClient(hostname, port, new AFPAuthenticator() {
			@Override
			public Map<AuthDetail, char[]> authenticate(AuthDetail... require) {
				Map<AuthDetail, char[]> m = new HashMap<AFPAuthenticator.AuthDetail, char[]>();
				if (DefaultFileSystemConfigBuilder.getInstance().getUserAuthenticator(fsOptions) != null) {
					List<UserAuthenticationData.Type> types = new ArrayList<UserAuthenticationData.Type>();
					for (AuthDetail d : require) {
						switch (d) {
						case USERNAME:
							types.add(UserAuthenticationData.USERNAME);
							break;
						case PASSWORD:
							types.add(UserAuthenticationData.PASSWORD);
							break;
						default:
							break;
						}
					}
					UserAuthenticationData authData = UserAuthenticatorUtils.authenticate(fsOptions,
							types.toArray(new UserAuthenticationData.Type[0]));
					if (authData == null) {
						return null;
					}
					for (AuthDetail d : require) {
						switch (d) {
						case USERNAME:
							String username = UserAuthenticatorUtils.toString(
									UserAuthenticatorUtils.getData(authData, UserAuthenticationData.USERNAME, null));
							if (username == null || username.length() == 0)
								username = defUsername;
							if (username != null)
								m.put(AuthDetail.USERNAME, username.toCharArray());
							break;
						case PASSWORD:
							String password = UserAuthenticatorUtils.toString(
									UserAuthenticatorUtils.getData(authData, UserAuthenticationData.PASSWORD, null));
							if (password != null)
								m.put(AuthDetail.PASSWORD, password.toCharArray());
							break;
						default:
							break;
						}
					}
				}
				return m;
			}
		});
		client.setAuthenticationMethods(AFPFileSystemConfigBuilder.getInstance().getAuthenticationMethods(fsOptions));
		clients.add(new WeakReference<AFPClient>(client));
		return client;
	}

	@Override
	protected void addCapabilities(Collection<Capability> capabilities) {
		capabilities.addAll(AFPFileProvider.capabilities);
	}

	@Override
	protected FileObject createFile(AbstractFileName name) throws Exception {
		return new AFPFileObject((AFPFileName) name, this);
	}
}
