package com.maverick.vfs.rfbftp;

import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemOptions;

public class RFBFTPFileSystemConfigBuilder extends FileSystemConfigBuilder {
    private final static RFBFTPFileSystemConfigBuilder builder = new
            RFBFTPFileSystemConfigBuilder();

    public static RFBFTPFileSystemConfigBuilder getInstance() {
        return builder;
    }

    private RFBFTPFileSystemConfigBuilder() {
    }
	
    public void setClient(FileSystemOptions opts, RFBFTPClient sshClient) {
    	setParam(opts, "rfbClient", sshClient);
    }
    
    public RFBFTPClient getClient(FileSystemOptions opts) {
        return (RFBFTPClient) getParam(opts, "rfbClient");
    }

    protected Class getConfigClass() {
        return RFBFTPFileSystem.class;
    }
}
