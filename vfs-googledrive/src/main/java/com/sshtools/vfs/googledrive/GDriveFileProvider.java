package com.sshtools.vfs.googledrive;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.sshtools.vfs.googledrive.GDriveFileSystemConfigBuilder.Mode;

public class GDriveFileProvider extends AbstractOriginatingFileProvider {

	public final static Collection<Capability> capabilities = Collections
			.unmodifiableCollection(Arrays.asList(Capability.CREATE, Capability.DELETE,
					// Capability.RENAME,
					Capability.ATTRIBUTES, Capability.GET_TYPE, Capability.GET_LAST_MODIFIED, Capability.LIST_CHILDREN,
					Capability.READ_CONTENT, Capability.URI, Capability.WRITE_CONTENT));

	public final static UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = new UserAuthenticationData.Type[] {
			UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD };
	/** Global instance of the JSON factory. */
	public static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	private static FileSystemOptions defaultOptions = new FileSystemOptions();

	public static FileSystemOptions getDefaultFileSystemOptions() {
		return defaultOptions;
	}

	private Log logger = LogFactory.getLog(GDriveFileProvider.class);

	private static FileDataStoreFactory dataStoreFactory;

	static {
		File dir = new File(new File(System.getProperty("java.io.tmpdir")),
				"gdrive-dsf-" + System.getProperty("user.name") + "-" + System.currentTimeMillis());
		dir.deleteOnExit();
		try {
			dataStoreFactory = new FileDataStoreFactory(dir);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to initialise datastore factory.", e);
		}
	}

	public GDriveFileProvider() {
		super();
		setFileNameParser(GDriveFileNameParser.getInstance());
	}

	public static FileDataStoreFactory getDataStoreFactory() {
		return dataStoreFactory;
	}

	private Credential authorize(NetHttpTransport httpTransport, FileSystemOptions fsOptions, GenericFileName fileName)
			throws IOException {
		GoogleClientSecrets secrets = loadSecrets(fsOptions);

		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
				GDriveFileProvider.JSON_FACTORY, secrets, Collections.singleton(DriveScopes.DRIVE))
						.setDataStoreFactory(dataStoreFactory).build();

		String userId = ((GenericFileName) fileName).getUserName();

		UserAuthenticationData authData = null;
		authData = UserAuthenticatorUtils.authenticate(fsOptions, AUTHENTICATOR_TYPES);
		try {

			if (userId == null)
				userId = UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
						UserAuthenticationData.USERNAME, userId == null ? null : userId.toCharArray()));

			if (userId == null) {
				throw new IOException(
						"A user identifier is required. This may anything (no need to be your real Google username), it used to locate stored credentials.");
			}
			
			Credential credential = flow.loadCredential(userId);
			if (credential != null)
				return credential;

			String tokenResponseJson =UserAuthenticatorUtils
					.toString(UserAuthenticatorUtils.getData(authData, UserAuthenticationData.PASSWORD, null));

			if (tokenResponseJson == null || tokenResponseJson.length() == 0) {
				tokenResponseJson = ((GenericFileName) fileName).getPassword();
				if (tokenResponseJson != null) {
					tokenResponseJson =  URLDecoder.decode(tokenResponseJson, "UTF-8");
				}
			}

			if (userId == null || tokenResponseJson == null || userId.length() + tokenResponseJson.length() == 0) {
				throw new FileSystemException("Empty credentials");
			}

			TokenResponse response = JSON_FACTORY.createJsonParser(tokenResponseJson).parse(TokenResponse.class);
			return flow.createAndStoreCredential(response, userId);
		} finally {
			UserAuthenticatorUtils.cleanup(authData);
		}
	}

	private GoogleClientSecrets loadSecrets(FileSystemOptions fileSystemOptions) throws IOException {
		URL jsonClientIdResource = GDriveFileSystemConfigBuilder.getInstance().getClientIdJSON(fileSystemOptions);
		if (jsonClientIdResource == null)
			throw new IOException(
					"Missing client identifier configuration. See" + GDriveFileSystemConfigBuilder.class.getName());

		Reader reader = new InputStreamReader(jsonClientIdResource.openStream());
		try {
			return GoogleClientSecrets.load(JSON_FACTORY, reader);
		} finally {
			reader.close();
		}
	}

	protected FileSystem doCreateFileSystem(FileName fileName, FileSystemOptions fileSystemOptions)
			throws FileSystemException {

		FileSystemOptions fsOptions = fileSystemOptions != null ? fileSystemOptions : getDefaultFileSystemOptions();
		try {
			Mode mode = GDriveFileSystemConfigBuilder.getInstance().getMode(fsOptions);
			Drive drive;

			logger.info("Initialize Google Drive client (" + mode + ")");

			NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

			switch (mode) {
			case SERVICE:
				UserAuthenticationData authData = null;
				try {

					authData = UserAuthenticatorUtils.authenticate(fsOptions, AUTHENTICATOR_TYPES);

					String clientId = UserAuthenticatorUtils
							.toString(UserAuthenticatorUtils.getData(authData, UserAuthenticationData.USERNAME, null));

					if (clientId == null || clientId.length() == 0)
						clientId = ((GenericFileName) fileName).getUserName();

					String clientSecret = UserAuthenticatorUtils
							.toString(UserAuthenticatorUtils.getData(authData, UserAuthenticationData.PASSWORD, null));

					if (clientSecret == null || clientSecret.length() == 0)
						clientSecret = ((GenericFileName) fileName).getPassword();

					if (clientId == null || clientSecret == null || clientId.length() + clientSecret.length() == 0) {
						throw new FileSystemException("Empty credentials");
					}

					GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
					clientSecrets.getInstalled().setClientId(clientId);
					clientSecrets.getInstalled().setClientSecret(clientSecret);

					GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport,
							JSON_FACTORY, clientSecrets, Collections.singleton(DriveScopes.DRIVE_FILE))
									.setDataStoreFactory(dataStoreFactory).build();

					Credential auth = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
							.authorize("user");
					drive = new Drive.Builder(httpTransport, JSON_FACTORY, auth).setApplicationName("GDriveVFS/1.0")
							.build();

					return new GDriveFileSystem((GDriveFileName) fileName, drive, fsOptions, httpTransport);
				} finally {
					UserAuthenticatorUtils.cleanup(authData);
				}
			case OAUTH:
				drive = new Drive.Builder(httpTransport, JSON_FACTORY,
						authorize(httpTransport, fileSystemOptions, (GenericFileName) fileName))
								.setApplicationName("GDriveVFS/1.0").build();

				return new GDriveFileSystem((GDriveFileName) fileName, drive, fsOptions, httpTransport);
			default:
				throw new UnsupportedOperationException(String.format("Unknown mode %s", mode));
			}

		} catch (InvalidKeyException e) {
			throw new FileSystemException(e.getMessage(), e);
		} catch (IOException e) {
			throw new FileSystemException(e.getMessage(), e);
		} catch (GeneralSecurityException e) {
			throw new FileSystemException(e.getMessage(), e);
		} finally {
		}

	}

	public Collection<Capability> getCapabilities() {
		return capabilities;
	}
}
