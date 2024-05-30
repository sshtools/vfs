package com.sshtools.vfs.sftp;

import java.util.Objects;

import javax.net.SocketFactory;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticator;

import com.sshtools.client.KeyPairAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.SshConnection;
import com.sshtools.common.ssh.components.SshKeyPair;

public class SftpClientFactory {

	static SocketFactory socketFactory = null;

	public static final String OPT_PRIVATE_KEY = "privateKey";
	public static final String OPT_PASSPHRASE = "passphrase";
	
	public static void setSocketFactory(SocketFactory socketFactory) {
		SftpClientFactory.socketFactory = socketFactory;
	}

	public static SshConnection createConnection(String hostname, int port,
			String username, String password,
			FileSystemOptions fileSystemOptions) throws FileSystemException {

		// The file system options may already have a client
		SshConnection ssh = SftpFileSystemConfigBuilder.getInstance().getSshConnection(fileSystemOptions);
		if(ssh != null) {
			return ssh;
		}

		try {
			SshClient client = null;
			
			if (username == null || password == null) {
				UserAuthenticator ua = SftpFileSystemConfigBuilder.getInstance().getUserAuthenticator(fileSystemOptions);

				if(ua!=null) {
					UserAuthenticationData data = ua
							.requestAuthentication(new UserAuthenticationData.Type[] {
									UserAuthenticationData.USERNAME,
									UserAuthenticationData.PASSWORD });
					
					if(data==null) {
						throw new FileSystemException("vfs.provider.sftp/authentication-cancelled.error");
					}
							
					username = new String(data
							.getData(UserAuthenticationData.USERNAME));
					password = new String(data
							.getData(UserAuthenticationData.PASSWORD));
					
					client = new SshClient(hostname, port, username, password.toCharArray());
				}
			}	
			
			if(Objects.isNull(client) || !client.isAuthenticated()) {
				
				
				SshKeyPair pair = null;
				String privateKey = SftpFileSystemConfigBuilder.getInstance().getPrivateKey(fileSystemOptions);
				
				if(privateKey!=null && privateKey.trim().length() > 0) {
					String passphrase = SftpFileSystemConfigBuilder.getInstance().getPassphrase(fileSystemOptions);
					if(passphrase!=null && passphrase.trim().length() == 0) {
						passphrase = null;
					}
					try {
						pair = SshPrivateKeyFileFactory.parse(privateKey.getBytes("UTF-8")).toKeyPair(passphrase);
					} catch (Exception e) {
						throw new FileSystemException("vfs.provider.sftp/private-key-failed.error", e);
					}
				}
				
				if(Objects.isNull(ssh)) {
					client = new SshClient(hostname, port, username, pair);
				} else {
					client.authenticate(new KeyPairAuthenticator(pair), 30000);
				}
			}

			if (!client.isAuthenticated()) {
				throw new FileSystemException("vfs.provider.sftp/authentication-failed.error",
						new Object[] { username });
			}

			ssh = client.getConnection();
			
		} catch (FileSystemException fse) {
			throw fse;
		} catch (final Exception ex) {
			throw new FileSystemException("vfs.provider.sftp/connect.error",
					 ex, hostname);
		}

		return ssh;
	}
}
