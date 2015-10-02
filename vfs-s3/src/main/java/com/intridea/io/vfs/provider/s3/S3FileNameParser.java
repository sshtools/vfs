package com.intridea.io.vfs.provider.s3;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.HostFileNameParser;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.provider.VfsComponentContext;

/**
 * @author Matthias L. Jugel
 */
public class S3FileNameParser extends HostFileNameParser {

	/**
	 * S3 file name parser instance
	 */
	private static final S3FileNameParser instance = new S3FileNameParser();

	/**
	 * Gets singleton
	 * 
	 * @return
	 */
	public static S3FileNameParser getInstance() {
		return instance;
	}

	public S3FileNameParser() {
		super(0);
	}

	/**
	 * Parses URI and constructs S3 file name.
	 */
	@Override
	public FileName parseUri(final VfsComponentContext context,
			final FileName base, final String filename)
			throws FileSystemException {
		// StringBuilder name = new StringBuilder();
		//
		// String scheme = UriParser.extractScheme(filename, name);
		// UriParser.canonicalizePath(name, 0, name.length(), this);
		//
		// // Normalize separators in the path
		// UriParser.fixSeparators(name);
		//
		// // Normalise the path
		// FileType fileType = UriParser.normalisePath(name);
		//
		// // Extract bucket name
		// UriParser.
		// final String bucketName = UriParser.extractFirstElement(name);
		//
		// return new S3FileName(scheme, bucketName, name.toString(), fileType);

		final StringBuilder name = new StringBuilder();
		final Authority auth = extractToPath(filename, name);
		String scheme = auth.getScheme();
		if (auth.getUserName() == null) {
			scheme = UriParser.extractScheme(filename, name);
//			name.setLength(0);
			UriParser.canonicalizePath(name, 0, name.length(), this);
			UriParser.fixSeparators(name);
		}
		FileType fileType = UriParser.normalisePath(name);
		String path = name.toString();
		if (path.equals("")) {
			path = "/";
		}

		// return new GenericFileName(
		// auth.scheme,
		// auth.hostName,
		// auth.port,
		// defaultPort,
		// auth.userName,
		// auth.password,
		// path,
		// fileType);

		return new S3FileName(scheme, path, fileType);
	}

}
