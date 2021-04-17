package com.sshtools.vfs.azure;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;
import org.apache.commons.vfs2.provider.GenericFileName;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;

public class AzureFileProvider extends AbstractOriginatingFileProvider {

	public final static Collection<Capability> capabilities = Collections
			.unmodifiableCollection(Arrays.asList(Capability.CREATE, Capability.DELETE,
					// Capability.RENAME,
					Capability.ATTRIBUTES, Capability.GET_TYPE, Capability.GET_LAST_MODIFIED, Capability.LIST_CHILDREN,
					Capability.READ_CONTENT, Capability.URI, Capability.WRITE_CONTENT));

	public final static UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = new UserAuthenticationData.Type[] {
			UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD };

	private static FileSystemOptions defaultOptions = new FileSystemOptions();

	public static FileSystemOptions getDefaultFileSystemOptions() {
		return defaultOptions;
	}

	private Log logger = LogFactory.getLog(AzureFileProvider.class);

	public AzureFileProvider() {
		super();
		setFileNameParser(AzureFileNameParser.getInstance());
	}

	protected FileSystem doCreateFileSystem(FileName fileName, FileSystemOptions fileSystemOptions)
			throws FileSystemException {

		FileSystemOptions fsOptions = fileSystemOptions != null ? fileSystemOptions : getDefaultFileSystemOptions();
		UserAuthenticationData authData = null;
		CloudBlobClient service;
		try {
			authData = UserAuthenticatorUtils.authenticate(fsOptions, AUTHENTICATOR_TYPES);

			logger.info("Initialize Azure client");

			String account = UserAuthenticatorUtils
					.toString(UserAuthenticatorUtils.getData(authData, UserAuthenticationData.USERNAME, null));

			if (account == null || account.length() == 0)
				account = ((GenericFileName) fileName).getUserName();

			String key = UserAuthenticatorUtils
					.toString(UserAuthenticatorUtils.getData(authData, UserAuthenticationData.PASSWORD, null));

			if (key == null || key.length() == 0)
				key = ((GenericFileName) fileName).getPassword();

			if (account == null || key == null || account.length() + key.length() == 0) {
				throw new FileSystemException("Empty credentials");
			}
			String storageConnectionString = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s",
					account, key);
			CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

			service = storageAccount.createCloudBlobClient();

		} catch (InvalidKeyException e) {
			throw new FileSystemException(e.getMessage(), e);
		} catch (URISyntaxException e) {
			throw new FileSystemException(e.getMessage(), e);
		} finally {
			UserAuthenticatorUtils.cleanup(authData);
		}

		return new AzureFileSystem((AzureFileName) fileName, service, fsOptions);
	}

	public Collection<Capability> getCapabilities() {
		return capabilities;
	}
}
