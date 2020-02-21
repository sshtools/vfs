package com.sshtools.vfs.sftp;

import java.util.Objects;

import javax.net.SocketFactory;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;

import com.sshtools.client.PublicKeyAuthenticator;
import com.sshtools.client.SshClient;
import com.sshtools.common.publickey.SshPrivateKeyFileFactory;
import com.sshtools.common.ssh.components.SshKeyPair;

public class SftpClientFactory {

	static SocketFactory socketFactory = null;

	public static final String OPT_PRIVATE_KEY = "privateKey";
	public static final String OPT_PASSPHRASE = "passphrase";
	
	public static void setSocketFactory(SocketFactory socketFactory) {
		SftpClientFactory.socketFactory = socketFactory;
	}

	public static SshClient createConnection(String hostname, int port,
			String username, String password,
			FileSystemOptions fileSystemOptions) throws FileSystemException {

		// The file system options may already have a client
		SshClient ssh = SftpFileSystemConfigBuilder.getInstance().getSshClient(fileSystemOptions);
		if(ssh != null) {
			return ssh;
		}

		try {
			
			
			if (username == null || password == null) {
				UserAuthenticator ua = DefaultFileSystemConfigBuilder
						.getInstance().getUserAuthenticator(fileSystemOptions);

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
					
					ssh = new SshClient(hostname, port, username, password.toCharArray());
				}
			}	
			
			if(Objects.isNull(ssh) || !ssh.isAuthenticated()) {
				
				
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
					ssh = new SshClient(hostname, port, username, pair);
				} else {
					ssh.authenticate(new PublicKeyAuthenticator(pair), 30000);
				}
			}

			if (!ssh.isAuthenticated()) {
				throw new FileSystemException("vfs.provider.sftp/authentication-failed.error",
						new Object[] { username });
			}

		} catch (FileSystemException fse) {
			throw fse;
		} catch (final Exception ex) {
			throw new FileSystemException("vfs.provider.sftp/connect.error",
					 ex, hostname);
		}

		return ssh;
	}
}
