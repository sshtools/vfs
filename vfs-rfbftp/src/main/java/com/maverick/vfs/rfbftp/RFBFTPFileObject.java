package com.maverick.vfs.rfbftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.UriParser;

import com.sshtools.rfb.RFBFS;
import com.sshtools.rfbcommon.DefaultRFBFile;
import com.sshtools.rfbcommon.RFBFile;

public class RFBFTPFileObject extends AbstractFileObject {
	private final RFBFTPFileSystem fileSystem;
	private RFBFile attrs;

	protected RFBFTPFileObject(final AbstractFileName name,
			final RFBFTPFileSystem fileSystem) {
		super(name, fileSystem);
		this.fileSystem = fileSystem;
	}

	protected FileType doGetType() throws Exception {
		if (attrs == null) {
			return FileType.IMAGINARY;
		}
		if (attrs.isFolder()) {
			return FileType.FOLDER;
		}
		return FileType.FILE;
	}

	private void statSelf() throws Exception {
		attrs = getRFBFileSystem().stat(processPath(getName().getPathDecoded()));
	}

	private RFBFS getRFBFileSystem() throws IOException {
		return fileSystem.getClient().getDisplay().getEngine().getFileSystem();
	}

	// /
	// /c
	// /c/a
	// /d/b/e
	private String processPath(String path) {
		return path;
	}

	protected void doCreateFolder() throws Exception {
		if(!getRFBFileSystem().mkdir(processPath(getName().getPathDecoded()))) {
			throw new FileSystemException("vfs.provider.rfb/create-folder.error");
		}
	}

	protected long doGetLastModifiedTime() throws Exception {
		if (attrs == null) {
			throw new FileSystemException(
					"vfs.provider.rfb/get-last-modified-time.error");
		}
		return attrs.getLastWriteTime();
	}

	protected boolean doSetLastModifiedTime(final long modtime)
			throws Exception {
		throw new UnsupportedOperationException();
	}

	protected void doDelete() throws Exception {
		if(!getRFBFileSystem().rm(processPath(getName().getPathDecoded()))) {
			throw new FileSystemException("vfs.provider.rfb/delete.error");
		}
	}

	protected void doRename(FileObject newfile) throws Exception {
		String oldName = processPath(getName().getPathDecoded());
		String newName = processPath(newfile.getName().getPathDecoded());
		if (oldName.equals(newName)) {
			throw new FileSystemException(
					"vfs.provider.rfb/rename-identical-files",
					new Object[] { newName });
		}
		getRFBFileSystem().mv(oldName, newName);
	}

	protected String[] doListChildren() throws Exception {
		// List the contents of the folder
		RFBFS rfbfs = getRFBFileSystem();
		String pathDecoded = getName().getPathDecoded();
		String path = processPath(pathDecoded);
		RFBFile[] array = rfbfs.list(path);
		if (array == null) {
			throw new FileSystemException(
					"vfs.provider.rfb/list-children.error");
		}
		// Extract the child names
		final ArrayList children = new ArrayList();
		for (int i = 0; i < array.length; i++) {
			if (!array[i].getName().equals(".")
					&& !array[i].getName().equals("..")) {
				children.add(array[i].getName());
			}
		}
		return UriParser.encode((String[]) children.toArray(new String[children
				.size()]));
	}

	protected Map doGetAttributes() throws Exception {
		if (attrs == null) {
			throw new FileSystemException(
					"vfs.provider.sftp/get-attributes.error");
		}
		final Map attributes = new HashMap();
		attributes.put("accessedTime", Long.valueOf(attrs.getLastAccessTime()));
		attributes.put("creationTime", Long.valueOf(attrs.getCreationTime()));
		return attributes;
	}

	protected long doGetContentSize() throws Exception {
		if (attrs == null) {
			throw new FileSystemException(
					"vfs.provider.rfb/get-content-size.error");
		}
		return attrs.getSize();
	}

	protected InputStream doGetInputStream() throws Exception {
		return getRFBFileSystem().receive(processPath(getName().getPathDecoded()), 0);
	}

	protected void doAttach() throws Exception {
		super.doAttach();
		statSelf();
	}

	protected void doDetach() throws Exception {
		super.doDetach();
		attrs = null;
	}

	protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
		return getRFBFileSystem().send(processPath(getName().getPathDecoded()), true, bAppend ? attrs.getSize() : 0);
	}

	protected boolean doIsHidden() throws Exception {
		return getName().getBaseName().startsWith(".");
	}
}
