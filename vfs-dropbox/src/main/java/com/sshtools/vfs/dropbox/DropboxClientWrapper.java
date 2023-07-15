/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sshtools.vfs.dropbox;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.UploadUploader;
import com.dropbox.core.v2.files.WriteMode;

/**
 * A wrapper to the FTPClient to allow automatic reconnect on connection loss.
 * <br />
 * I decided to not to use eg. noop() to determine the state of the connection
 * to avoid unnecesary server round-trips.
 * 
 * @author <a href="http://commons.apache.org/vfs/team-list.html">Commons VFS
 *         team</a>
 */
class DropboxClientWrapper implements DropboxClient {
	private final DropboxFileName root;
	private final FileSystemOptions fileSystemOptions;
	private DbxClientV2 dbxClient;

	DropboxClientWrapper(final DropboxFileName root, final FileSystemOptions fileSystemOptions) throws FileSystemException {
		this.root = root;
		this.fileSystemOptions = fileSystemOptions;
		getDbxClient(); // fail-fast
	}

	public DropboxFileName getRoot() {
		return root;
	}

	public FileSystemOptions getFileSystemOptions() {
		return fileSystemOptions;
	}

	private DbxClientV2 createClient() throws FileSystemException {
		UserAuthenticationData authData = null;
		try {
			UserAuthenticator ua = DefaultFileSystemConfigBuilder.getInstance().getUserAuthenticator(fileSystemOptions);
			UserAuthenticationData data = ua
					.requestAuthentication(new UserAuthenticationData.Type[] { UserAuthenticationData.PASSWORD });
			if (data == null) {
				throw new Exception("vfs.provider.sftp/authentication-cancelled.error");
			}

			DbxRequestConfig config = DbxRequestConfig.newBuilder("DropboxVfs/4.0").withUserLocaleFrom(Locale.getDefault()).build();
			
			String accessToken = new String(data.getData(UserAuthenticationData.PASSWORD));
			if (accessToken.equals("")) {
				throw new Exception("vfs.provider.sftp/authentication-cancelled.error");
			}
			
			String[] app = new String(data.getData(UserAuthenticationData.DOMAIN)).split("|");			

			String refreshToken = new String(data.getData(UserAuthenticationData.USERNAME));

			if(app!=null && app.length==2 && refreshToken!=null && !"".equals(refreshToken)) {
				return new DbxClientV2(config, new DbxCredential(accessToken, -1L, refreshToken, app[0], app[1]));
			} else {
				return new DbxClientV2(config, accessToken);
			}
		} catch (Exception e) {
			throw new FileSystemException(e);
		} finally {
			UserAuthenticatorUtils.cleanup(authData);
		}
	}

	private DbxClientV2 getDbxClient() throws FileSystemException {
		if (dbxClient == null) {
			dbxClient = createClient();
		}
		return dbxClient;
	}

	public boolean isConnected() throws FileSystemException {
		return dbxClient != null;
	}

	public void disconnect() throws IOException {
		dbxClient = null;
	}

	public List<Metadata> listFiles(String relPath) throws IOException {
		// try {
		// VFS-210: return getFtpClient().listFiles(relPath);
		try {
			return listFilesInDirectory(relPath.equals("/") ? "" : relPath);
		} catch (DbxException e) {
			throw new IOException("Dropbox I/O error.", e);
		}
		// }
		// TODO is this really necessary
		// catch (IOException e) {
		// disconnect();
		// DbxEntry[] files = listFilesInDirectory(relPath);
		// return files;
		// }
	}

	private List<Metadata> listFilesInDirectory(String relPath) throws IOException, DbxException {
		List<Metadata> files = new LinkedList<Metadata>();
		// Get files and folder metadata from Dropbox root directory
		DbxClientV2 client = getDbxClient();
		ListFolderResult result = client.files().listFolder(relPath);
		while (true) {
			files.addAll(result.getEntries());
			if (!result.getHasMore()) {
				break;
			}
			result = client.files().listFolderContinue(result.getCursor());
		}
		return files;
	}

	public boolean removeDirectory(String relPath) throws IOException {
		try {
			getDbxClient().files().delete(relPath);
			return true;
		} catch (DbxException e) {
			throw new IOException(e);
		}
	}

	public boolean deleteFile(String relPath) throws IOException {
		try {
			getDbxClient().files().delete(relPath);
			return true;
		} catch (DbxException e) {
			throw new IOException(e);
		}
	}

	public boolean rename(String oldName, String newName) throws IOException {
		try {
			return getDbxClient().files().move(oldName, newName) != null;
		} catch (DbxException e) {
			throw new IOException(e);
		}
	}

	public boolean makeDirectory(String relPath) throws IOException {
		try {
			return getDbxClient().files().createFolder(relPath) != null;
		} catch (DbxException e) {
			throw new IOException(e);
		}
	}

	public InputStream retrieveFileStream(String relPath) throws IOException {
		try {
			return getDbxClient().files().downloadBuilder(relPath).start().getInputStream();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public OutputStream storeFileStream(String relPath) throws IOException {
		try {
			final UploadUploader upload = getDbxClient().files().uploadBuilder(relPath).withMode(WriteMode.OVERWRITE).start();
			return new FilterOutputStream(upload.getOutputStream()) {
				@Override
				public void close() throws IOException {
					super.close();
					try {
						upload.finish();
					} catch (UploadErrorException e) {
						throw new IOException("Failed to upload.", e);
					} catch (DbxException e) {
						throw new IOException("Failed to upload.", e);
					}
				}
			};
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public boolean abort() throws IOException {
		try {
			// imario@apache.org: 2005-02-14
			// it should be better to really "abort" the transfer, but
			// currently I didnt manage to make it work - so lets "abort" the
			// hard way.
			// return getFtpClient().abort();
			disconnect();
			return true;
		} catch (IOException e) {
			disconnect();
		}
		return true;
	}
}
