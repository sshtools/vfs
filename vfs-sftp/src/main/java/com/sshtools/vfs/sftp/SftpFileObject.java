package com.sshtools.vfs.sftp;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.util.RandomAccessMode;

import com.sshtools.client.sftp.SftpClient;
import com.sshtools.client.sftp.SftpFile;
import com.sshtools.common.sftp.SftpFileAttributes;
import com.sshtools.common.sftp.SftpStatusException;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.SshIOException;
import com.sshtools.common.util.UnsignedInteger32;
import com.sshtools.common.util.UnsignedInteger64;

public class SftpFileObject extends AbstractFileObject<SftpFileSystem> {

	public final static String ATTR_UID = "uid";
	public final static String ATTR_GID = "gid";
	public final static String ATTR_PERMISSIONS = "permissions";

	private final SftpFileSystem fs;
	private SftpFileAttributes attrs;

	protected SftpFileObject(final AbstractFileName name,
			final SftpFileSystem fs) {
		super(name, fs);
		this.fs = fs;
	}

	@Override
	protected void doSetAttribute(String attrName, Object value)
			throws Exception {
		if (attrs == null) {
			throw new FileSystemException(
					"vfs.provider.sftp/get-last-modified-time.error");
		}
		final SftpClient sftp = fs.getClient();
		try {
			if (attrName.equals(ATTR_UID)) {
				attrs.setUID(String.valueOf(value));
			} else if (attrName.equals(ATTR_GID)) {
				attrs.setGID(String.valueOf(value));
			} else if (attrName.equals(ATTR_PERMISSIONS)) {
				int intValue = ((Integer) value).intValue();
				if (intValue != -1) {
					attrs.setPermissions(new UnsignedInteger32(intValue));
				}
			} else {
				super.doSetAttribute(attrName, value);
			}
			sftp.getSubsystemChannel().setAttributes(
					getName().getPathDecoded(), attrs);
		} finally {
			fs.putClient(sftp);
		}
	}

	protected FileType doGetType() throws Exception {

		if (attrs == null) {
			return FileType.IMAGINARY;
		}

		if (attrs.isDirectory()) {
			return FileType.FOLDER;
		}
		return FileType.FILE;
	}

	private void statSelf() throws Exception {
		final SftpClient sftp = fs.getClient();
		try {
			attrs = sftp.stat(getName().getPathDecoded());
		} catch (final SftpStatusException e) {
			// Does not exist
			attrs = null;
		} finally {
			fs.putClient(sftp);
		}
	}

	protected void doCreateFolder() throws Exception {
		final SftpClient sftp = fs.getClient();
		try {
			sftp.mkdir(getName().getPathDecoded());
			statSelf();
		} finally {
			fs.putClient(sftp);
		}
	}

	protected long doGetLastModifiedTime() throws Exception {
		if (attrs == null) {
			throw new FileSystemException(
					"vfs.provider.sftp/get-last-modified-time.error");
		}

		long lastModifiedSeconds = attrs.getModifiedTime().longValue();
		return lastModifiedSeconds * 1000L;
	}

	protected boolean doSetLastModifiedTime(final long modtime)
			throws Exception {
		if (attrs == null) {
			throw new FileSystemException(
					"vfs.provider.sftp/get-last-modified-time.error");
		}
		final SftpClient sftp = fs.getClient();
		try {
			attrs.setTimes(null, new UnsignedInteger64(modtime / 1000L));
			sftp.getSubsystemChannel().setAttributes(
					getName().getPathDecoded(), attrs);
			return true;
		} finally {
			fs.putClient(sftp);
		}
	}

	protected void doDelete() throws Exception {
		final SftpClient sftp = fs.getClient();
		try {
			sftp.rm(getName().getPathDecoded());
		} finally {
			fs.putClient(sftp);
		}
	}

	protected void doRename(FileObject newfile) throws Exception {
		final SftpClient sftp = fs.getClient();
		try {

			String oldName = getName().getPathDecoded();
			String newName = newfile.getName().getPathDecoded();
			if (oldName.equals(newName)) {
				throw new FileSystemException(
						"vfs.provider.sftp/rename-identical-files",
						new Object[] { newName });
			}
			sftp.rename(oldName, newName);
		} finally {
			fs.putClient(sftp);
		}
	}

	protected String[] doListChildren() throws Exception {
		// List the contents of the folder
		final SftpFile[] array;
		final SftpClient sftp = fs.getClient();
		try {
			array = sftp.ls(getName().getPathDecoded());
		} finally {
			fs.putClient(sftp);
		}
		if (array == null) {
			throw new FileSystemException(
					"vfs.provider.sftp/list-children.error");
		}

		// Extract the child names
		final ArrayList<String> children = new ArrayList<String>();
		for (int i = 0; i < array.length; i++) {
			if (!array[i].getFilename().equals(".")
					&& !array[i].getFilename().equals("..")) {
				children.add(array[i].getFilename());
			}
		}
		return UriParser.encode((String[]) children.toArray(new String[children
				.size()]));
	}

	protected Map<String, Object> doGetAttributes() throws Exception {
		if (attrs == null) {
			throw new FileSystemException(
					"vfs.provider.sftp/get-attributes.error");
		}
		final Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("accessedTime",
				Long.valueOf(attrs.getAccessedTime().longValue()));
		attributes.put("creationTime",
				Long.valueOf(attrs.getCreationTime().longValue()));
		attributes.put("gid", attrs.getGID());
		attributes.put("maskString", attrs.getMaskString());
		attributes.put("permissions",
				Integer.valueOf(attrs.getPermissions().intValue()));
		attributes.put("permissionsString", attrs.getPermissionsString());
		attributes.put("uid", attrs.getUID());
		attributes.put("block", Boolean.valueOf(attrs.isBlock()));
		attributes.put("character", Boolean.valueOf(attrs.isCharacter()));
		attributes.put("fifo", Boolean.valueOf(attrs.isFifo()));
		attributes.put("link", Boolean.valueOf(attrs.isLink()));
		attributes.put("socket", Boolean.valueOf(attrs.isSocket()));
		return attributes;
	}

	protected long doGetContentSize() throws Exception {
		if (attrs == null) {
			throw new FileSystemException(
					"vfs.provider.sftp/get-content-size.error");
		}
		return attrs.getSize().longValue();
	}

	protected RandomAccessContent doGetRandomAccessContent(
			final RandomAccessMode mode) throws Exception {
		return new SftpRandomAccessContent(this, mode);
	}

	/**
	 * Creates an input stream to read the file content from.
	 */
	InputStream getInputStream(long filePointer) throws IOException {
		final SftpClient sftp = fs.getClient();
		try {
			final InputStream inputStream = sftp.getInputStream(getName()
					.getPathDecoded(), filePointer);
			return new FilterInputStream(inputStream) {
				@Override
				public void close() throws IOException {
					try {
						super.close();
					} finally {
						fs.putClient(sftp);
					}
				}
			};
		} catch (SftpStatusException e) {
			fs.putClient(sftp);
			throw new IOException(e.getMessage());
		} catch (SshException e) {
			fs.putClient(sftp);
			throw new SshIOException(e);
		} catch (Exception e) {
			fs.putClient(sftp);
			IOException i = new IOException();
			i.initCause(e);
			throw i;
		}

	}

	protected InputStream doGetInputStream() throws Exception {
		final SftpClient sftp = fs.getClient();
		try {
			final InputStream inputStream = sftp.getInputStream(getName()
					.getPathDecoded());
			return new FilterInputStream(inputStream) {
				@Override
				public void close() throws IOException {
					try {
						super.close();
					} finally {
						fs.putClient(sftp);
					}
				}
			};
		} catch (Exception e) {
			fs.putClient(sftp);
			throw e;
		}
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
		final SftpClient sftp = fs.getClient();
		try {
			final OutputStream outputStream = sftp.getOutputStream(getName()
					.getPathDecoded());
			return new FilterOutputStream(outputStream) {
				public void write(byte b[], int off, int len)
						throws IOException {
					outputStream.write(b, off, len);
				}

				@Override
				public void close() throws IOException {
					try {
						super.close();
					} finally {
						// These two methods must be call in this order, or the
						// client get/put will be 'nested',
						// which the original client being closed on leaving
						// this method. This would not be an
						// issue or ordinary connections, as a new client will
						// just get created, but it means
						// it will failed when the client is shared.
						fs.putClient(sftp);
						try {
							statSelf();
						} catch (Exception e) {
						}
					}
				}
			};
		} catch (Exception e) {
			fs.putClient(sftp);
			statSelf();
			throw e;
		}

	}

	protected boolean doIsHidden() throws Exception {
		return getName().getBaseName().startsWith(".");
	}
}
