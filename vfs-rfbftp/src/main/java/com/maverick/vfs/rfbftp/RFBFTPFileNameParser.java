package com.maverick.vfs.rfbftp;

import org.apache.commons.vfs2.provider.FileNameParser;
import org.apache.commons.vfs2.provider.URLFileNameParser;


public class RFBFTPFileNameParser extends URLFileNameParser
{
    private final static RFBFTPFileNameParser instance = new RFBFTPFileNameParser();

    public RFBFTPFileNameParser() {
        super(0);
    }

    public static FileNameParser getInstance() {
        return instance;
    }
}
