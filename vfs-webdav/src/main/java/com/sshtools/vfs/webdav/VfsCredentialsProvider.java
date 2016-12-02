package com.sshtools.vfs.webdav;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.util.Args;

public class VfsCredentialsProvider implements CredentialsProvider {

	private final BasicCredentialsProvider internal;
	private final FileSystemOptions fsOpts;
	private final URLFileName rootName;

	public VfsCredentialsProvider(FileSystemOptions fsOpts, URLFileName rootName) {
		super();
		this.rootName = rootName;
		this.fsOpts = fsOpts;
		this.internal = new BasicCredentialsProvider();
	}

	@Override
	public void setCredentials(final AuthScope authscope, final Credentials credentials) {
		internal.setCredentials(authscope, credentials);
	}

	@Override
	public Credentials getCredentials(final AuthScope authscope) {
		Args.notNull(authscope, "Auth scope");
		final Credentials localcreds = internal.getCredentials(authscope);
		if (localcreds != null) {
			return localcreds;
		}
		if (authscope.getHost() != null) {
			UserAuthenticationData authData = null;
			try {
				List<UserAuthenticationData.Type> types = new ArrayList<UserAuthenticationData.Type>(
						Arrays.asList(UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD));
				if (AuthSchemes.NTLM.equalsIgnoreCase(authscope.getScheme())) {
					types.add(0, UserAuthenticationData.DOMAIN);
				}
				authData = UserAuthenticatorUtils.authenticate(fsOpts,
						types.toArray(new UserAuthenticationData.Type[0]));
				String username = UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
						UserAuthenticationData.USERNAME, UserAuthenticatorUtils.toChar(rootName.getUserName())));
				String password = UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
						UserAuthenticationData.PASSWORD, UserAuthenticatorUtils.toChar(rootName.getPassword())));
				String domain = null;
				if (AuthSchemes.NTLM.equalsIgnoreCase(authscope.getScheme())) {
					domain = UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
							UserAuthenticationData.DOMAIN, UserAuthenticatorUtils.toChar(rootName.getPassword())));
				}
				if (domain != null) {
					return new NTCredentials(username, password, null, domain);
				}
				return new UsernamePasswordCredentials(username, password);
			} finally {
				UserAuthenticatorUtils.cleanup(authData);
			}

		}
		return null;
	}

	@Override
	public void clear() {
		internal.clear();
	}

}
