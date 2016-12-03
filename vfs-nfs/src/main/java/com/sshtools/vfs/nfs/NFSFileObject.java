package com.sshtools.vfs.nfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;

import com.emc.ecs.nfsclient.nfs.io.Nfs3File;
import com.emc.ecs.nfsclient.nfs.io.NfsFileInputStream;
import com.emc.ecs.nfsclient.nfs.io.NfsFileOutputStream;
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3;

public class NFSFileObject extends AbstractFileObject<NFSFileSystem> {
	final static Log LOG = LogFactory.getLog(NFSFileObject.class);

	public final static int DEFAULT_BLOB_INCREMENT = 1024;
	public final static int DEFAULT_BLOB_SIZE = 1024;

	private Nfs3 drive;

	private Nfs3File file;

	public NFSFileObject(AbstractFileName fileName, NFSFileSystem fileSystem, Nfs3 nfs3) throws FileSystemException {
		this(fileName, fileSystem, nfs3, null);
	}

	public NFSFileObject(AbstractFileName fileName, NFSFileSystem fileSystem, Nfs3 nfs3, Nfs3File file)
			throws FileSystemException {
		super(fileName, fileSystem);
		this.drive = nfs3;
		this.file = file;
	}

	@Override
	protected void doCreateFolder() throws URISyntaxException, IOException {
		getNfsFile().mkdir();
	}

	@Override
	protected void doDelete() throws Exception {
		getNfsFile().delete();
	}

	protected Nfs3File getNfsFile() throws IOException {
		Nfs3File file = this.file;
		if (file == null)
			file = new Nfs3File(drive, getName().getPath());
		return file;
	}

	@Override
	protected long doGetContentSize() throws Exception {
		return getNfsFile().length();
	}

	@Override
	protected void doAttach() throws Exception {
		file = getNfsFile();
	}

	@Override
	protected void doDetach() throws Exception {
		file = null;
	}

	@Override
	protected InputStream doGetInputStream() throws Exception {
		return new NfsFileInputStream(getNfsFile());
	}

	@Override
	protected long doGetLastModifiedTime() throws Exception {
		return getNfsFile().lastModified();
	}

	@Override
	protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
		Nfs3File nfsFile = getNfsFile();
		return new NfsFileOutputStream(nfsFile, bAppend ? nfsFile.length() : 0, 1);
	}

	@Override
	protected FileType doGetType() throws Exception {
		Nfs3File nfsFile = getNfsFile();
		if (nfsFile.exists()) {
			if (nfsFile.isDirectory())
				return FileType.FOLDER;
			else
				return FileType.FILE;
		}
		return FileType.IMAGINARY;
	}

	@Override
	protected boolean doIsHidden() throws Exception {
		return getName().getBaseName().startsWith(".");
	}

	@Override
	protected boolean doIsExecutable() throws Exception {
		return getNfsFile().canExecute();
	}

	@Override
	protected boolean doIsReadable() throws Exception {
		return getNfsFile().canRead();
	}

	@Override
	protected boolean doIsWriteable() throws Exception {
		return getNfsFile().canModify();
	}

	@Override
	protected FileObject[] doListChildrenResolved() throws Exception {
		List<FileObject> l = new LinkedList<FileObject>();
		for (Nfs3File s : getNfsFile().listFiles()) {
			NFSFileName nfsname = (NFSFileName) getName();
			NFSFileName n = new NFSFileName(getName().getScheme(), nfsname.getHostName(), nfsname.getMount(),
					nfsname.getUserName(), nfsname.getPassword(),
					getName().getPath() + (getName().getPath().endsWith("/") ? "" : "/") + s.getName(),
					s.isDirectory() ? FileType.FOLDER : FileType.FILE);
			NFSFileObject e = new NFSFileObject(n, (NFSFileSystem) getFileSystem(), drive, s);
			l.add(e);
		}
		return l.toArray(new FileObject[0]);
	}

	@Override
	protected String[] doListChildren() throws Exception {
		return getNfsFile().list().toArray(new String[0]);
	}

	@Override
	protected void doRemoveAttribute(String attrName) throws Exception {
		throw new FileSystemException("Removal of attributes not supported on this file.");
	}

	@Override
	protected void doRename(FileObject newfile) throws Exception {
		if (newfile instanceof NFSFileObject) {
			getNfsFile().rename(((NFSFileObject) newfile).getNfsFile());
		} else {
			if (newfile.getName().isDescendent(getName()))
				throw new IOException("Cannot rename to a descendent of self.");
			newfile.copyFrom(this, new AllFileSelector());
			delete(new AllFileSelector());
		}
	}

}
