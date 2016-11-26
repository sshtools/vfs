package com.sshtools.vfs.rfbftp;

import javax.net.SocketFactory;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;

import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBEncoding;
import com.sshtools.rfb.RFBEventHandler;

public class RFBFTPClientFactory {
	static SocketFactory socketFactory = null;

	public static RFBFTPClient createConnection(String hostname, int port, String password, FileSystemOptions fileSystemOptions)
			throws FileSystemException {
		// The file system options may already have a client
		RFBFTPClient rfb = RFBFTPFileSystemConfigBuilder.getInstance().getClient(fileSystemOptions);
		if (rfb != null) {
			return rfb;
		}
		/**
		 * TODO: use the FileSystemOptions variable to retrieve some SSH context
		 * settings
		 */
		try {
			if (password == null) {
				UserAuthenticator ua = DefaultFileSystemConfigBuilder.getInstance().getUserAuthenticator(fileSystemOptions);
				UserAuthenticationData data = ua
					.requestAuthentication(new UserAuthenticationData.Type[] { UserAuthenticationData.PASSWORD });
				if (data == null) {
					throw new Exception("vfs.provider.sftp/authentication-cancelled.error");
				}
				password = new String(data.getData(UserAuthenticationData.PASSWORD));
			}
			final String fPassword = password;
			RFBContext context = new RFBContext();
			if (port < 5800) {
				port += 5900;
			}
			rfb = new RFBFTPClient(context, hostname, port, new RFBEventHandler() {
				public void resized(int width, int height) {
				}

				public String passwordAuthenticationRequired() {
					return fPassword;
				}

				public void encodingChanged(RFBEncoding currentEncoding) {
				}

				public void disconnected() {
				}

				public void connected() {
				}
			});
		} catch (FileSystemException fse) {
			throw fse;
		} catch (final Exception ex) {
			throw new FileSystemException("vfs.provider.rfb/connect.error", new Object[] { hostname }, ex);
		}
		return rfb;
	}
}
