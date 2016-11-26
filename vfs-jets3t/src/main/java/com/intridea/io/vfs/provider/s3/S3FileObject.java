package com.intridea.io.vfs.provider.s3;

import static org.apache.commons.vfs2.FileName.SEPARATOR;
import static org.apache.commons.vfs2.FileName.SEPARATOR_CHAR;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.util.MonitorOutputStream;
import org.jets3t.service.Constants;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.CanonicalGrantee;
import org.jets3t.service.acl.GrantAndPermission;
import org.jets3t.service.acl.GranteeInterface;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageOwner;
import org.jets3t.service.utils.Mimetypes;

import com.intridea.io.vfs.operations.acl.Acl;
import com.intridea.io.vfs.operations.acl.IAclGetter;

/**
 * Implementation of the virtual S3 file system object using the Jets3t library.
 * Based on Matthias Jugel code
 * http://thinkberg.com/svn/moxo/trunk/src/main/java/com/thinkberg/moxo/
 * 
 * @author Marat Komarov
 * @author Matthias L. Jugel
 */
public class S3FileObject extends AbstractFileObject {
	/**
	 * Amazon S3 service
	 */
	private final S3Service service;

	/**
	 * Amazon S3 bucket
	 */
	private S3Bucket bucket;

	/**
	 * Amazon S3 object
	 */
	private S3Object object;

	/**
	 * True when content attached to file
	 */
	private boolean attached = false;

	/**
	 * True when content downloaded. It's an extended flag to
	 * <code>attached</code>.
	 */
	private boolean downloaded = false;

	/**
	 * Local cache of file content
	 */
	private File cacheFile;

	/**
	 * Amazon file owner. Used in ACL
	 */
	private StorageOwner fileOwner;

	/**
	 * Class logger
	 */
	private Log logger = LogFactory.getLog(S3FileObject.class);

	private boolean exists;

	public S3FileObject(AbstractFileName fileName, S3FileSystem fileSystem, S3Service service, S3Bucket bucket)
			throws FileSystemException {

		super(fileName, fileSystem);
		this.service = service;
		this.bucket = bucket;
	}

	@Override
	protected void doAttach() throws IOException, NoSuchAlgorithmException {
		if (!attached) {
			try {
				// Do we have file with name?
				object = service.getObject(bucket.getName(), getS3Key());

				logger.info("Attach file to S3 Object: " + object);

				attached = true;
				exists = true;
				return;
			} catch (Exception e) {
				// No, we don't
			}

			try {
				// Do we have folder with that name?
				object = service.getObject(bucket.getName(), getS3Key() + FileName.SEPARATOR);

				logger.info("Attach folder to S3 Object: " + object);

				attached = true;
				exists = true;
				return;
			} catch (Exception e) {
				// No, we don't
			}

			// Create a new
			if (object == null) {
				object = new S3Object();
				object.setBucketName(bucket.getName());
				object.setKey(getS3Key());
				object.setLastModifiedDate(new Date());
				logger.info(String.format("Attach file to S3 Object: %s", object));
				downloaded = true;
				attached = true;
				exists = false;
			}

		}
	}

	@Override
	protected void doDetach() throws Exception {
		if (this.attached) {
			this.object = null;
			if (this.cacheFile != null) {
				this.cacheFile.delete();
				this.cacheFile = null;
			}
			this.downloaded = false;
			this.attached = false;
		}
	}

	@Override
	protected void doDelete() throws Exception {
		System.out.println("Deleting " + this.object.getKey() + " from " + this.bucket.getName());
		this.service.deleteObject(this.bucket.getName(), this.object.getKey());
	}

	@Override
	protected void doRename(FileObject newfile) throws Exception {

		S3Object newObject = new S3Object();
		newObject.setBucketName(bucket.getName());
		newObject.setLastModifiedDate(new Date());

		if (getType().equals(FileType.FOLDER)) {
			newObject.setKey(getS3Key(newfile.getName()) + FileName.SEPARATOR);
			newObject.setDataInputStream(getEmptyInputStream());
		} else {
			service.copyObject(bucket.getName(), object.getKey(), bucket.getName(), newObject, false);
		}
		service.putObject(bucket.getName(), newObject);
		service.deleteObject(bucket.getName(), object.getKey());
	}

	@Override
	protected void doCreateFolder() throws S3ServiceException {

		if (logger.isDebugEnabled()) {
			logger.debug("Create new folder in bucket [" + ((bucket != null) ? bucket.getName() : "null")
					+ "] with key [" + ((object != null) ? object.getKey() : "null") + "]");
		}

		if (object == null) {
			return;
		}
		object.setDataInputStream(getEmptyInputStream());
		object.setKey(object.getKey() + FileName.SEPARATOR);

		service.putObject(bucket.getName(), object);
	}

	@Override
	protected long doGetLastModifiedTime() throws Exception {
		return this.object.getLastModifiedDate().getTime();
	}

	@Override
	protected boolean doSetLastModifiedTime(final long modtime) throws Exception {
		// TODO: last modified date will be changed only when content changed
		this.object.setLastModifiedDate(new Date(modtime));
		return true;
	}

	@Override
	protected InputStream doGetInputStream() throws Exception {
		downloadOnce();
		return Channels.newInputStream(getCacheFileChannel());
	}

	@Override
	protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
		return new S3OutputStream(Channels.newOutputStream(getCacheFileChannel()), this.service, this.object);
	}

	// @Override
	// protected FileType doGetType() throws Exception {
	// /*
	// * Anything that doesn't exist can be treated like an empty file or
	// * folder in S3
	// */
	// return FileType.FILE_OR_FOLDER;
	// }

	@Override
	protected FileType doGetType() throws Exception {
		if (!exists) {
			return FileType.IMAGINARY;
		}
		if (null == object.getContentType()) {
			return FileType.FOLDER;
		}

		if ("".equals(object.getKey()) || object.getKey().endsWith("/")) {
			return FileType.FOLDER;
		}

		return FileType.FILE;
	}

	// protected String[] XXXXXdoListChildren() throws Exception {
	// String path = getS3Key();
	// // make sure we add a '/' slash at the end to find children
	// if (!"".equals(path)) {
	// path = path + "/";
	// }
	//
	// // S3Object[] children = this.service.listObjects(this.bucket.getName(),
	// // path, "/");
	//
	// final ListObjectsRequest listObjectRequest = new ListObjectsRequest()
	// .withBucketName(this.bucket.getName()).withPrefix(path)
	// .withDelimiter("/");
	// final ObjectListing objectListing = service
	// .listObjects(listObjectRequest);
	// List<String> c = new ArrayList<String>();
	// for (final S3ObjectSummary objectSummary : objectListing
	// .getObjectSummaries()) {
	// final String key = objectSummary.getKey();
	// if (isImmediateDescendant(path, key)) {
	// final String relativePath = getRelativePath(path, key);
	// c.add(relativePath);
	// }
	// }
	// return c.toArray(new String[0]);
	// }

	@Override
	protected String[] doListChildren() throws Exception {
		String path = object.getKey();
		// make sure we add a '/' slash at the end to find children
		if (path.equals("") || (!path.endsWith(SEPARATOR))) {
			path = path + "/";
		}

		S3Object[] children = service.listObjects(bucket.getName(), path, "/",
				Constants.DEFAULT_OBJECT_LIST_CHUNK_SIZE);

		List<String> childrenNames = new ArrayList<String>(children.length);

		for (S3Object child : children) {
			if (!child.getKey().equals(path)) {
				// strip path from name (leave only base name)
				final String stripPath = child.getKey().substring(path.length());

				// Only one slash in the end OR no slash at all
				if ((stripPath.endsWith(SEPARATOR)
						&& (stripPath.indexOf(SEPARATOR_CHAR) == stripPath.lastIndexOf(SEPARATOR_CHAR)))
						|| (stripPath.indexOf(SEPARATOR_CHAR) == (-1))) {
					childrenNames.add(stripPath);
				}
			}
		}

		return childrenNames.toArray(new String[childrenNames.size()]);
	}

	private String getRelativePath(final String parent, final String child) {
		if (!child.startsWith(parent)) {
			throw new IllegalArgumentException("Invalid child '" + child + "' for parent '" + parent + "'");
		}
		// a String.replace() also would be fine here
		final int parentLen = parent.length();
		return child.substring(parentLen);
	}

	private boolean isImmediateDescendant(final String parent, final String child) {
		if (!child.startsWith(parent)) {
			// maybe we just should return false
			throw new IllegalArgumentException("Invalid child '" + child + "' for parent '" + parent + "'");
		}
		final int parentLen = parent.length();
		final String childWithoutParent = child.substring(parentLen);
		if (childWithoutParent.contains("/")) {
			return false;
		}
		return true;
	}

	@Override
	protected long doGetContentSize() throws Exception {
		return this.object.getContentLength();
	}

	// Utility methods

	/**
	 * Download S3 object content and save it in temporary file. Do it only if
	 * object was not already downloaded.
	 */
	private void downloadOnce() throws FileSystemException {
		if (!this.downloaded) {
			final String failedMessage = "Failed to download S3 Object %s. %s";
			String objectPath = ((S3FileName) getName()).getPathAfterBucket();
			try {
				S3Object obj = this.service.getObject(this.bucket.getName(), getS3Key());
				this.logger.info(String.format("Downloading S3 Object: %s", objectPath));
				InputStream is = obj.getDataInputStream();
				if (obj.getContentLength() > 0) {
					ReadableByteChannel rbc = Channels.newChannel(is);
					FileChannel cacheFc = getCacheFileChannel();
					cacheFc.transferFrom(rbc, 0, obj.getContentLength());
					cacheFc.close();
					rbc.close();
				} else {
					is.close();
				}
			} catch (ServiceException e) {
				throw new FileSystemException(String.format(failedMessage, objectPath, e.getMessage()), e);
			} catch (IOException e) {
				throw new FileSystemException(String.format(failedMessage, objectPath, e.getMessage()), e);
			}

			this.downloaded = true;
		}

	}

	/**
	 * Create an S3 key from a commons-vfs path. This simply strips the slash
	 * from the beginning if it exists.
	 * 
	 * @return the S3 object key
	 */
	private String getS3Key() {
		String path = ((S3FileName) getName()).getPathAfterBucket();
		if ("".equals(path)) {
			return path;
		} else {
			return path.substring(1);
		}
	}

	/**
	 * Get or create temporary file channel for file cache
	 * 
	 * @return
	 * @throws IOException
	 */
	private FileChannel getCacheFileChannel() throws IOException {
		if (this.cacheFile == null) {
			this.cacheFile = File.createTempFile("scalr.", ".s3");
		}
		return new RandomAccessFile(this.cacheFile, "rw").getChannel();
	}

	// ACL extension methods

	/**
	 * Returns S3 file owner. Loads it from S3 if needed.
	 */
	private StorageOwner getS3Owner() throws ServiceException {
		if (this.fileOwner == null) {
			AccessControlList s3Acl = getS3Acl();
			this.fileOwner = s3Acl.getOwner();
		}
		return this.fileOwner;
	}

	/**
	 * Get S3 ACL list
	 * 
	 * @return
	 * @throws ServiceException
	 */
	private AccessControlList getS3Acl() throws ServiceException {
		String key = getS3Key();
		return "".equals(key) ? this.service.getBucketAcl(this.bucket.getName())
				: this.service.getObjectAcl(this.bucket.getName(), key);
	}

	/**
	 * Put S3 ACL list
	 * 
	 * @param s3Acl
	 * @throws Exception
	 */
	private void putS3Acl(AccessControlList s3Acl) throws Exception {
		String key = getS3Key();
		// Determine context. Object or Bucket
		if ("".equals(key)) {
			this.bucket.setAcl(s3Acl);
		} else {
			// Before any operations with object it must be attached
			doAttach();
			// Put ACL to S3
			this.object.setAcl(s3Acl);
		}
	}

	/**
	 * Returns access control list for this file.
	 * 
	 * VFS interfaces doesn't provide interface to manage permissions. ACL can
	 * be accessed through {@link FileObject#getFileOperations()} Sample:
	 * <code>file.getFileOperations().getOperation(IAclGetter.class)</code>
	 * 
	 * @see {@link FileObject#getFileOperations()}
	 * @see {@link IAclGetter}
	 * 
	 * @return Current Access control list for a file
	 * @throws FileSystemException
	 */
	public Acl getAcl() throws FileSystemException {
		Acl myAcl = new Acl();
		AccessControlList s3Acl;
		try {
			s3Acl = getS3Acl();
		} catch (ServiceException e) {
			throw new FileSystemException(e);
		}

		// Get S3 file owner
		StorageOwner owner = s3Acl.getOwner();
		this.fileOwner = owner;

		// Read S3 ACL list and build VFS ACL.
		for (GrantAndPermission item : s3Acl.getGrantAndPermissions()) {

			// Map enums to jets3t ones
			Permission perm = item.getPermission();
			Acl.Permission[] rights;
			if (perm.equals(Permission.PERMISSION_FULL_CONTROL)) {
				rights = Acl.Permission.values();
			} else if (perm.equals(Permission.PERMISSION_READ)) {
				rights = new Acl.Permission[1];
				rights[0] = Acl.Permission.READ;
			} else if (perm.equals(Permission.PERMISSION_WRITE)) {
				rights = new Acl.Permission[1];
				rights[0] = Acl.Permission.WRITE;
			} else {
				// Skip unknown permission
				this.logger.error(String.format("Skip unknown permission %s", perm));
				continue;
			}

			// Set permissions for groups
			if (item.getGrantee() instanceof GroupGrantee) {
				GroupGrantee grantee = (GroupGrantee) item.getGrantee();
				if (GroupGrantee.ALL_USERS.equals(grantee)) {
					// Allow rights to GUEST
					myAcl.allow(Acl.Group.EVERYONE, rights);
				} else if (GroupGrantee.AUTHENTICATED_USERS.equals(grantee)) {
					// Allow rights to AUTHORIZED
					myAcl.allow(Acl.Group.AUTHORIZED, rights);
				}
			} else if (item.getGrantee() instanceof CanonicalGrantee) {
				CanonicalGrantee grantee = (CanonicalGrantee) item.getGrantee();
				if (grantee.getIdentifier().equals(owner.getId())) {
					// The same owner and grantee understood as OWNER group
					myAcl.allow(Acl.Group.OWNER, rights);
				}
			}

		}

		return myAcl;
	}

	/**
	 * Returns access control list for this file.
	 * 
	 * VFS interfaces doesn't provide interface to manage permissions. ACL can
	 * be accessed through {@link FileObject#getFileOperations()} Sample:
	 * <code>file.getFileOperations().getOperation(IAclGetter.class)</code>
	 * 
	 * @see {@link FileObject#getFileOperations()}
	 * @see {@link IAclGetter}
	 * 
	 * @param acl
	 * @throws FileSystemException
	 */
	public void setAcl(Acl acl) throws FileSystemException {

		// Create empty S3 ACL list
		AccessControlList s3Acl = new AccessControlList();

		// Get file owner
		StorageOwner owner;
		try {
			owner = getS3Owner();
		} catch (ServiceException e) {
			throw new FileSystemException(e);
		}
		s3Acl.setOwner(owner);

		// Iterate over VFS ACL rules and fill S3 ACL list
		Hashtable<Acl.Group, Acl.Permission[]> rules = acl.getRules();
		Enumeration<Acl.Group> keys = rules.keys();
		Acl.Permission[] allRights = Acl.Permission.values();
		while (keys.hasMoreElements()) {
			Acl.Group group = keys.nextElement();
			Acl.Permission[] rights = rules.get(group);

			if (rights.length == 0) {
				// Skip empty rights
				continue;
			}

			// Set permission
			Permission perm;
			if (Arrays.equals(rights, allRights)) {
				// Use ArrayUtils instead of native equals method.
				// JRE1.6 enum[].equals behavior is very strange:
				// Two equal by elements arrays are not equal
				// Yeah, AFAIK its like that for any array.
				perm = Permission.PERMISSION_FULL_CONTROL;
			} else if (acl.isAllowed(group, Acl.Permission.READ)) {
				perm = Permission.PERMISSION_READ;
			} else if (acl.isAllowed(group, Acl.Permission.WRITE)) {
				perm = Permission.PERMISSION_WRITE;
			} else {
				this.logger.error(String.format("Skip unknown set of rights %s", rights.toString()));
				continue;
			}

			// Set grantee
			GranteeInterface grantee;
			if (group.equals(Acl.Group.EVERYONE)) {
				grantee = GroupGrantee.ALL_USERS;
			} else if (group.equals(Acl.Group.AUTHORIZED)) {
				grantee = GroupGrantee.AUTHENTICATED_USERS;
			} else if (group.equals(Acl.Group.OWNER)) {
				grantee = new CanonicalGrantee(owner.getId());
			} else {
				this.logger.error(String.format("Skip unknown group %s", group));
				continue;
			}

			// Grant permission
			s3Acl.grantPermission(grantee, perm);
		}

		// Put ACL to S3
		try {
			putS3Acl(s3Acl);
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	/**
	 * Special JetS3FileObject output stream. It saves all contents in temporary
	 * file, onClose sends contents to S3.
	 * 
	 * @author Marat Komarov
	 */
	private class S3OutputStream extends MonitorOutputStream {

		private S3Service service;

		private S3Object object;

		public S3OutputStream(OutputStream out, S3Service service, S3Object object) {
			super(out);
			this.service = service;
			this.object = object;
		}

		@Override
		protected void onClose() throws IOException {
			this.object.setContentType(Mimetypes.getInstance().getMimetype(this.object.getKey()));
			try {
				this.object.setDataInputStream(Channels.newInputStream(getCacheFileChannel()));
				this.service.putObject(this.object.getBucketName(), this.object);
			} catch (ServiceException e) {
				IOException ioe = new IOException();
				ioe.initCause(e);
				throw ioe;
			}
		}
	}

	private InputStream getEmptyInputStream() {
		return new ByteArrayInputStream(new byte[0]);
	}

	private String getS3Key(FileName fileName) {
		String path = ((S3FileName) getName()).getPathAfterBucket();
		if (!"".equals(path)) {
			path = path.substring(1);
		}
		if (fileName.getType().equals(FileType.FOLDER) && !path.endsWith("/")) {
			path += "/";
		}
		return path;
	}
}
