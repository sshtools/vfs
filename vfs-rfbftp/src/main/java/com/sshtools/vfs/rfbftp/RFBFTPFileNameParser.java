package com.sshtools.vfs.rfbftp;

import org.apache.commons.vfs2.provider.FileNameParser;
import org.apache.commons.vfs2.provider.GenericURLFileNameParser;


public class RFBFTPFileNameParser extends GenericURLFileNameParser
{
    private final static RFBFTPFileNameParser instance = new RFBFTPFileNameParser();

    public RFBFTPFileNameParser() {
        super(0);
    }

    public static FileNameParser getInstance() {
        return instance;
    }
}
