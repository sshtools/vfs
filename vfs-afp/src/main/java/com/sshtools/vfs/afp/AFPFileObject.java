package com.sshtools.vfs.afp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileObject;

import com.sshtools.jafp.client.AFPClient;
import com.sshtools.jafp.client.AFPFile;
import com.sshtools.jafp.client.AFPFileInputStream;
import com.sshtools.jafp.client.AFPFileOutputStream;
import com.sshtools.jafp.client.AFPVolume;
import com.sshtools.jafp.common.AFPDirectoryInfo;
import com.sshtools.jafp.common.AFPFileInfo;
import com.sshtools.jafp.common.Utility;
import com.sshtools.jafp.server.AFPNodeInfo;

public class AFPFileObject extends AbstractFileObject<AFPFileSystem> {
	final static Log LOG = LogFactory.getLog(AFPFileObject.class);

	public enum Scope {
		NETWORK, SERVER, VOLUME, FILE
	}

	public final static int DEFAULT_BLOB_INCREMENT = 1024;
	public final static int DEFAULT_BLOB_SIZE = 1024;
	private Scope scope;
	private AFPFile file;
	private AFPNodeInfo info;
	private List<InetSocketAddress> hosts;
	private List<AFPVolume> volumes;
	private AFPVolume volume;
	private AFPClient client;

	public AFPFileObject(AFPFileName fileName, AFPFileSystem fileSystem) {
		this(fileName, fileSystem, null);
	}

	public AFPFileObject(AFPFileName fileName, AFPFileSystem fileSystem, AFPFile file) {
		super(fileName, fileSystem);
		this.file = file;
		determineScope();
	}

	protected AFPFileName getAFPName() {
		return (AFPFileName) getName();
	}

	protected void determineScope() {
		if (getAFPName().getHostName() == null) {
			scope = Scope.NETWORK;
		} else if (getAFPName().getVolume() == null) {
			scope = Scope.SERVER;
		} else {
			String p = getAFPName().getPath();
			StringTokenizer t = new StringTokenizer(p, FileName.SEPARATOR, false);
			if (t.countTokens() == 0)
				scope = Scope.VOLUME;
			else
				scope = Scope.FILE;
		}
	}

	@Override
	protected void doCreateFolder() throws URISyntaxException, IOException {
	}

	@Override
	protected void doDelete() throws Exception {
	}

	@Override
	protected long doGetContentSize() throws Exception {
		switch (scope) {
		case FILE:
			return info instanceof AFPFileInfo ? ((AFPFileInfo) info).getShortDataForkLen() : 0;
		default:
			return 0;
		}
	}

	@Override
	protected void doAttach() throws Exception {
		switch (scope) {
		case NETWORK: {
			hosts = new ArrayList<InetSocketAddress>();
			JmDNS dns = ((AFPFileSystem) getFileSystem()).getJmDns();
			for (ServiceInfo info : dns.list("_afpovertcp._tcp.local.")) {
				for (String addr : info.getHostAddresses()) {
					try {
						InetAddress addrObj = InetAddress.getByName(addr);
						if (addrObj instanceof Inet6Address) {
							LOG.warn(String.format("Skipped IPV6 address %s as commons VFS does not support IPV6 URI.", addr));
						} else {
							hosts.add(new InetSocketAddress(addrObj.getHostName(), info.getPort()));
						}
					} catch (UnknownHostException fse) {
						LOG.error(String.format("Skipping %s, failed to resolve.", addr), fse);
					}
				}
			}
			break;
		}
		case SERVER:
			volumes = getClient().list();
			break;
		default:
			if (getParent() instanceof AFPFileObject && ((AFPFileObject) getParent()).volume != null) {
				volume = ((AFPFileObject) getParent()).volume;
			} else if (getParent() instanceof AFPFileObject && ((AFPFileObject) getParent()).volumes != null) {
				for (AFPVolume v : ((AFPFileObject) getParent()).volumes) {
					if (v.getName().equals(getAFPName().getVolume())) {
						volume = v;
						break;
					}
				}
			}
			if (this.volume == null) {
				volume = getClient().get(getAFPName().getVolume());
			}
			if (!getName().getPath().equals("/")) {
				if (file == null) {
					file = new AFPFile(getName().getPath(), volume);
				} else {
					file.refresh();
				}
				info = file.getInfo();
			}
			break;
		}
	}

	protected AFPClient getClient() throws FileSystemException {
		if (client == null && getParent() instanceof AFPFileObject
				&& ((AFPFileObject) getParent()).getAFPName().getHostName() != null) {
			return ((AFPFileObject) getParent()).getClient();
		} else if (client != null) {
			return client;
		}
		client = ((AFPFileSystem) getFileSystem()).getClient(getAFPName().getHostName(), getAFPName().getUserName(),
				getAFPName().getPort(), getFileSystem().getFileSystemOptions());
		return client;
	}

	@Override
	protected void doDetach() throws Exception {
		switch (scope) {
		case NETWORK:
			hosts = null;
			break;
		case SERVER:
			volumes = null;
			break;
		default:
			volume = null;
			info = null;
			break;
		}
	}

	@Override
	protected InputStream doGetInputStream() throws Exception {
		switch (scope) {
		case FILE:
			return new AFPFileInputStream(file);
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	protected long doGetLastModifiedTime() throws Exception {
		switch (scope) {
		case FILE:
			return info instanceof AFPFileInfo ? Utility.afp2unixTime(((AFPFileInfo) info).getModifiedDate()) : 0;
		default:
			return 0;
		}
	}

	@Override
	protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
		switch (scope) {
		case FILE:
			return new AFPFileOutputStream(file, bAppend);
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	protected FileType doGetType() throws Exception {
		switch (scope) {
		case NETWORK:
		case SERVER:
		case VOLUME:
			return FileType.FOLDER;
		case FILE:
			if (info instanceof AFPFileInfo)
				return FileType.FILE;
			else if (info instanceof AFPDirectoryInfo)
				return FileType.FOLDER;
			return FileType.IMAGINARY;
		default:
			throw new UnsupportedOperationException();
		}
	}

	// @Override
	// protected boolean doIsHidden() throws Exception {
	// return getName().getBaseName().startsWith(".");
	// }
	//
	// @Override
	// protected boolean doIsExecutable() throws Exception {
	// return getNfsFile().canExecute();
	// }
	//
	// @Override
	// protected boolean doIsReadable() throws Exception {
	// return getNfsFile().canRead();
	// }
	//
	// @Override
	// protected boolean doIsWriteable() throws Exception {
	// return getNfsFile().canModify();
	// }
	//
	@Override
	protected FileObject[] doListChildrenResolved() throws Exception {
		switch (scope) {
		case NETWORK:
			List<FileObject> a = new ArrayList<FileObject>();
			for (InetSocketAddress v : hosts) {
				AFPFileName afn = new AFPFileName("afp", v.getHostName(), v.getPort(), getAFPName().getUserName(),
						getAFPName().getPassword(), null, null, FileType.FOLDER);
				FileObject fo = getFileSystem().resolveFile(afn);
				a.add(fo);
			}
			return a.toArray(new FileObject[0]);
		case SERVER:
			a = new ArrayList<FileObject>();
			for (AFPVolume v : volumes) {
				AFPFileName afn = new AFPFileName("afp", getAFPName().getHostName(), getAFPName().getPort(),
						getAFPName().getUserName(), getAFPName().getPassword(), v.getName(), null, FileType.FOLDER);
				FileObject fo = getFileSystem().resolveFile(afn);
				a.add(fo);
			}
			return a.toArray(new FileObject[0]);
		case VOLUME:
			a = new ArrayList<FileObject>();
			for (AFPFile f : volume.listFiles()) {
				AFPFileName afn = new AFPFileName("afp", getAFPName().getHostName(), getAFPName().getPort(),
						getAFPName().getUserName(), getAFPName().getPassword(), volume.getName(), "/" + f.getName(),
						FileType.FOLDER);
				FileObject fo = getFileSystem().resolveFile(afn);
				a.add(fo);
			}
			return a.toArray(new FileObject[0]);
		case FILE:
			a = new ArrayList<FileObject>();
			for (AFPFile f : file.listFiles()) {
				AFPFileName afn = new AFPFileName("afp", getAFPName().getHostName(), getAFPName().getPort(),
						getAFPName().getUserName(), getAFPName().getPassword(), volume.getName(),
						getName().getPathDecoded() + FileName.SEPARATOR + f.getName(), FileType.FOLDER);
				FileObject fo = getFileSystem().resolveFile(afn);
				a.add(fo);
			}
			return a.toArray(new FileObject[0]);
		default:
			return super.doListChildrenResolved();
		}
	}

	@Override
	protected String[] doListChildren() throws Exception {
		switch (scope) {
		default:
			throw new UnsupportedOperationException();
		}
	}

	@Override
	protected void doRemoveAttribute(String attrName) throws Exception {
		throw new FileSystemException("Removal of attributes not supported on this file.");
	}

	@Override
	protected void doRename(FileObject newfile) throws Exception {
		// if (newfile instanceof AFPFileObject) {
		// getAFPFile().rename(((AFPFileObject) newfile).getAFPFile());
		// } else {
		// if (newfile.getName().isDescendent(getName()))
		// throw new IOException("Cannot rename to a descendent of self.");
		// newfile.copyFrom(this, new AllFileSelector());
		// delete(new AllFileSelector());
		// }
	}
}
