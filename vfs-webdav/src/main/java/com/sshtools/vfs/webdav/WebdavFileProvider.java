package com.sshtools.vfs.webdav;

import java.net.ProxySelector;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;

import com.github.sardine.Sardine;
import com.github.sardine.impl.SardineImpl;

/**
 * Sardine-backed Webdav file provider
 * 
 * @author ndx
 *
 */
public class WebdavFileProvider extends AbstractOriginatingFileProvider {
	/** The authenticator types used by the WebDAV provider. */
	public static final UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = new UserAuthenticationData.Type[] {
			UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD };

	/** The capabilities of the WebDAV provider */
	protected static final Collection<Capability> capabilities = Collections
			.unmodifiableCollection(Arrays.asList(new Capability[] { Capability.CREATE, Capability.DELETE,
					Capability.RENAME, Capability.GET_TYPE, Capability.LIST_CHILDREN, Capability.READ_CONTENT,
					Capability.URI, Capability.WRITE_CONTENT, Capability.GET_LAST_MODIFIED, Capability.ATTRIBUTES,
					// Capability.RANDOM_ACCESS_READ,
					Capability.DIRECTORY_READ_CONTENT, }));

	private SSLContext sslContext;

	public WebdavFileProvider() {
		super();

		setFileNameParser(WebdavFileNameParser.getInstance());
	}

	/**
	 * Creates a {@link FileSystem}. Creates in fact a Sardine (with various
	 * required elements for auth and proxy handling) configured to connect to
	 * described host
	 * 
	 * @see org.apache.commons.vfs2.impl.DefaultFileSystemManager#resolveFile(FileObject,
	 *      String, FileSystemOptions)
	 */
	@Override
	protected FileSystem doCreateFileSystem(final FileName name, final FileSystemOptions fileSystemOptions)
			throws FileSystemException {
		final URLFileName rootName = (URLFileName) name;
		// Create the file system
		FileSystemOptions fsOpts = (fileSystemOptions == null) ? new FileSystemOptions() : fileSystemOptions;

		UserAuthenticationData authData = null;
		if (WebdavFileSystemConfigBuilder.getInstance().isPreemptiveAuth(fsOpts)) {
			try {
				authData = UserAuthenticatorUtils.authenticate(fsOpts, AUTHENTICATOR_TYPES);
				String username = UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
						UserAuthenticationData.USERNAME, UserAuthenticatorUtils.toChar(rootName.getUserName())));
				String password = UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
						UserAuthenticationData.PASSWORD, UserAuthenticatorUtils.toChar(rootName.getPassword())));
				WebdavFileSystem returned = new WebdavFileSystem(rootName, createSardine(fsOpts, username, password, rootName),
						fsOpts);
				// TODO find a way to make sure server filesystem is OK
				return returned;
			} finally {
				UserAuthenticatorUtils.cleanup(authData);
			}
		} else {
			return new WebdavFileSystem(rootName, createSardine(fsOpts, null, null, rootName), fsOpts);
		}
	}

	/**
	 * Create a sardine instance with the given auth and the right set of http
	 * params (including the crazy timeouts)
	 * 
	 * @param fsOpts
	 * @param username
	 * @param password
	 * @return
	 */
	private Sardine createSardine(final FileSystemOptions fsOpts, String username, String password, final URLFileName rootName) {
		return new SardineImpl(username, password) {

			@Override
			protected HttpClientBuilder configure(ProxySelector selector, CredentialsProvider credentials) {

				if (!WebdavFileSystemConfigBuilder.getInstance().isPreemptiveAuth(fsOpts)) {
					credentials = new VfsCredentialsProvider(fsOpts, rootName);
				}
				
				HttpClientBuilder builder = super.configure(selector, credentials);
				builder.setDefaultRequestConfig(RequestConfig.custom()
						// Only selectively enable this for PUT but not all
						// entity enclosing methods
						.setExpectContinueEnabled(false)
						.setConnectTimeout(WebdavFileSystemConfigBuilder.getInstance().getConnectTimeout(fsOpts))
						.setConnectionRequestTimeout(
								WebdavFileSystemConfigBuilder.getInstance().getSocketTimeout(fsOpts))
						.build());
				return builder;
			}

			@Override
			protected ConnectionSocketFactory createDefaultSecureSocketFactory() {
				TrustManager trustManager = WebdavFileSystemConfigBuilder.getInstance().getTrustManager(fsOpts);
				HostnameVerifier hostnameVerifier = WebdavFileSystemConfigBuilder.getInstance()
						.getHostnameVerifier(fsOpts);
				if (trustManager == null && hostnameVerifier == null) {
					return super.createDefaultSecureSocketFactory();
				} else {
					try {
						if (trustManager == null || hostnameVerifier == null)
							throw new IllegalStateException(
									"If hostname verifier is set, so must the trust manager be. And vice versa.");
						if (sslContext == null) {
							sslContext = SSLContext.getInstance("TLS");
							TrustManager[] tms = new TrustManager[] { trustManager };
							sslContext.init(null, tms, new SecureRandom());
						}
						return new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
					} catch (Exception e) {
						throw new IllegalStateException("Failed to initialise SSL.", e);
					}
				}
			}
		};
	}

	@Override
	public FileSystemConfigBuilder getConfigBuilder() {
		return WebdavFileSystemConfigBuilder.getInstance();
	}

	public Collection<Capability> getCapabilities() {
		return capabilities;
	}

	public FileName parseUri(FileName base, String uri) throws FileSystemException {
		if (getFileNameParser() != null) {
			return getFileNameParser().parseUri(getContext(), base, uri);
		}

		throw new FileSystemException("vfs.provider/filename-parser-missing.error");
		// return GenericFileName.parseUri(getFileNameParser(), uri, 0);
	}
}
