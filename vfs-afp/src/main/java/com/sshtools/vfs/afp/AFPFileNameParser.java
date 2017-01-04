package com.sshtools.vfs.afp;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.FileNameParser;
import org.apache.commons.vfs2.provider.HostFileNameParser;
import org.apache.commons.vfs2.provider.URLFileNameParser;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponentContext;
import org.apache.commons.vfs2.util.Cryptor;
import org.apache.commons.vfs2.util.CryptorFactory;

public class AFPFileNameParser extends URLFileNameParser {
	private static final AFPFileNameParser INSTANCE = new AFPFileNameParser();

	public AFPFileNameParser() {
		super(AFPFileName.DEFAULT_PORT);
	}

	public static FileNameParser getInstance() {
		return INSTANCE;
	}

	@Override
	protected Authority extractToPath(final String uri, final StringBuilder name) throws FileSystemException {
		final AFPAuthority auth = new AFPAuthority();
		// Extract the scheme
		auth.setScheme(UriParser.extractScheme(uri, name));
		// Expecting "//"
		if (name.length() < 2 || name.charAt(0) != '/' || name.charAt(1) != '/') {
			throw new FileSystemException("vfs.provider/missing-double-slashes.error", uri);
		}
		name.delete(0, 2);
		// Extract userinfo, and split into username and password
		final String userInfo = extractUserInfo(name);
		final String userName;
		final String password;
		if (userInfo != null) {
			final int idx = userInfo.indexOf(':');
			if (idx == -1) {
				userName = userInfo;
				password = null;
			} else {
				userName = userInfo.substring(0, idx);
				password = userInfo.substring(idx + 1);
			}
		} else {
			userName = null;
			password = null;
		}
		auth.setUserName(UriParser.decode(userName));
		auth.setPassword(UriParser.decode(password));
		if (auth.getPassword() != null && auth.getPassword().startsWith("{") && auth.getPassword().endsWith("}")) {
			try {
				final Cryptor cryptor = CryptorFactory.getCryptor();
				auth.setPassword(cryptor.decrypt(auth.getPassword().substring(1, auth.getPassword().length() - 1)));
			} catch (final Exception ex) {
				throw new FileSystemException("Unable to decrypt password", ex);
			}
		}
		
		// TODO hmmm. not entirely sure about this
		if(name.toString().startsWith("//"))
			name.delete(0, 2);
		
		// Extract hostname, and normalise (lowercase)
		
		final String hostName = extractHostName(name);
		// Extract port
		if(hostName != null) {
			auth.setHostName(hostName.toLowerCase());
			auth.setPort(extractPort(name, uri));
		}
		// Expecting '/' or empty name
		if (name.length() > 0 && name.charAt(0) != '/') {
			throw new FileSystemException("vfs.provider/missing-hostname-path-sep.error", uri);
		}
		return auth;
	}

	@Override
	public FileName parseUri(final VfsComponentContext context, final FileName base, final String filename)
			throws FileSystemException {
		final StringBuilder name = new StringBuilder();
		// Extract the scheme and authority parts
		final Authority auth = extractToPath(filename, name);
		String username = auth.getUserName();
		// Decode and adjust separators
		UriParser.canonicalizePath(name, 0, name.length(), this);
		UriParser.fixSeparators(name);
		// Extract the share
		String share = UriParser.decode(UriParser.extractFirstElement(name));
		if (share == null || share.length() == 0) {
			// TODO allow this, and list volumes as read only file objects
			// throw new
			// FileSystemException("vfs.provider.smb/missing-share-name.error",
			// filename);
		} else if (share != null && share.startsWith("?"))
			share = share.substring(1);
		if("".equals(share))
			share = null;
		
		// Normalise the path. Do this after extracting the share name,
		// to deal with things like smb://hostname/share/..
		final FileType fileType = UriParser.normalisePath(name);
		String path = name.toString();
		if(path == null || path.equals("") || path.equals("/"))
			path = null;
		return new AFPFileName(auth.getScheme(), auth.getHostName(), auth.getPort(), username, auth.getPassword(), share, path,
				fileType);
	}
	
	class AFPAuthority extends HostFileNameParser.Authority {
		
	}
}