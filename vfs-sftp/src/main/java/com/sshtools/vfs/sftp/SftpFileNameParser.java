package com.sshtools.vfs.sftp;

import org.apache.commons.vfs2.provider.FileNameParser;
import org.apache.commons.vfs2.provider.URLFileNameParser;


@SuppressWarnings("deprecation")
public class SftpFileNameParser extends URLFileNameParser
{
    private final static SftpFileNameParser instance = new SftpFileNameParser();

    public SftpFileNameParser() {
        super(22);
    }

    public static FileNameParser getInstance() {
        return instance;
    }
}
