package com.sshtools.vfs.sftp;

import java.io.File;

import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.common.knownhosts.HostKeyVerification;
import com.sshtools.common.ssh.SshConnection;

public class SftpFileSystemConfigBuilder extends DefaultFileSystemConfigBuilder {
    private final static SftpFileSystemConfigBuilder builder = new
            SftpFileSystemConfigBuilder();

    public static SftpFileSystemConfigBuilder getInstance() {
        return builder;
    }

    private SftpFileSystemConfigBuilder() {
    }

    public void setKnownHosts(FileSystemOptions opts, File sshdir) {
        setParam(opts, "knownHosts", sshdir);
    }

    public File getKnownHosts(FileSystemOptions opts) {
        return (File) getParam(opts, "knownHosts");
    }

	public void setHostKeyVerification(FileSystemOptions opts, HostKeyVerification hostKeyVerification) {
		setParam(opts, "hostKeyVerification", hostKeyVerification);
	}
	public HostKeyVerification getHostKeyVerification(FileSystemOptions opts) {
		return (HostKeyVerification)getParam(opts, "hostKeyVerification");
	}
	
	public void setSftpClient(FileSystemOptions opts, SftpClient sftpClient) {
		setParam(opts, "sftpClient", sftpClient);
	}
	
	public SftpClient getSftpClient(FileSystemOptions opts) {
		return (SftpClient) getParam(opts, "sftpClient");
	}
	
    public void setSshConnection(FileSystemOptions opts, SshConnection sshClient) {
    	setParam(opts, "sshConnection", sshClient);
    }
    
    public SshConnection getSshConnection(FileSystemOptions opts) {
        return (SshConnection) getParam(opts, "sshConnection");
    }
    
    public void setCharset(FileSystemOptions opts, String charset) {
        setParam(opts, "charset", charset);
    }
    
    public String getCharset(FileSystemOptions opts) {
        return (String) getParam(opts, "charset");
    }

    public void setCompression(FileSystemOptions opts, String compression) {
        setParam(opts, "compression", compression);
    }

    public String getCompression(FileSystemOptions opts) {
        return (String) getParam(opts, "compression");
    }

    public String getPrivateKey(FileSystemOptions opts) {
    	return (String) getParam(opts, "privateKey");
    }
    
    public String getPassphrase(FileSystemOptions opts) {
    	return (String) getParam(opts, "passphrase");
    }
    
    public void setPrivateKey(FileSystemOptions opts, String privateKey) {
    	setParam(opts, "privateKey", privateKey);
    }

    public void setPassphrase(FileSystemOptions opts, String passphrase) {
    	setParam(opts, "passphrase", passphrase);
    }
    
    protected Class<SftpFileSystem> getConfigClass() {
        return SftpFileSystem.class;
    }
}
