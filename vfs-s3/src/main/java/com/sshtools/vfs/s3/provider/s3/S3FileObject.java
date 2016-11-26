package com.sshtools.vfs.s3.provider.s3;

import static org.apache.commons.vfs2.FileName.SEPARATOR;

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
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.util.MonitorOutputStream;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.internal.Mimetypes;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.Grantee;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sshtools.vfs.s3.operations.acl.Acl;
import com.sshtools.vfs.s3.operations.acl.IAclGetter;

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
	private final AmazonS3Client service;
	/**
	 * Amazon S3 bucket
	 */
	private Bucket bucket;
	/**
	 * Amazon S3 object
	 */
	private S3Object object;
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
	private Owner fileOwner;
	/**
	 * Class logger
	 */
	private Log logger = LogFactory.getLog(S3FileObject.class);
	private boolean exists;

	public S3FileObject(AbstractFileName fileName, S3FileSystem fileSystem, AmazonS3Client service, Bucket bucket)
			throws FileSystemException {
		super(fileName, fileSystem);
		this.service = service;
		this.bucket = bucket;
	}

	@Override
	protected void doAttach() throws IOException, NoSuchAlgorithmException {
		String s3Key = getS3Key();
		try {
			// Do we have folder with that name?
			object = service.getObject(bucket.getName(), s3Key + FileName.SEPARATOR);
			logger.info("Attach folder to S3 Object: " + object);
			exists = true;
			return;
		} catch (AmazonS3Exception ase) {
			if (ase.getStatusCode() != 404)
				throw ase;
		}
		try {
			// Do we have file with name?
			object = service.getObject(bucket.getName(), s3Key);
			logger.info("Attach file to S3 Object: " + object);
			exists = true;
			return;
		} catch (AmazonS3Exception ase) {
			if (ase.getStatusCode() != 404)
				throw ase;
			// No, we don't
		}
		// Create a new
		if (object == null) {
			object = new S3Object();
			object.setBucketName(bucket.getName());
			object.setKey(s3Key);
			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setLastModified(new Date());
			object.setObjectMetadata(objectMetadata);
			logger.info(String.format("Attach file to S3 Object: %s", object));
			downloaded = true;
			exists = false;
		}
	}

	@Override
	protected void doDetach() throws Exception {
		this.object = null;
		if (this.cacheFile != null) {
			this.cacheFile.delete();
			this.cacheFile = null;
		}
		this.downloaded = false;
	}

	@Override
	protected void doDelete() throws Exception {
		String s3Key = this.object.getKey();
		if (getType() == FileType.FOLDER && !s3Key.endsWith("/")) {
			s3Key += "/";
		}
		this.service.deleteObject(this.bucket.getName(), s3Key);
	}

	@Override
	protected void doRename(FileObject newfile) throws Exception {
		if (getType().equals(FileType.FOLDER)) {
			service.putObject(bucket.getName(), getS3Key(newfile.getName()) + FileName.SEPARATOR, getEmptyInputStream(),
					getEmptyMetadata());
		} else {
			service.copyObject(bucket.getName(), object.getKey(), bucket.getName(), getS3Key(newfile.getName()));
		}
		service.deleteObject(bucket.getName(), object.getKey());
	}

	@Override
	protected void doCreateFolder() throws AmazonServiceException {
		if (logger.isDebugEnabled()) {
			logger.debug("Create new folder in bucket [" + ((bucket != null) ? bucket.getName() : "null") + "] with key ["
					+ ((object != null) ? object.getKey() : "null") + "]");
		}
		if (object == null) {
			return;
		}
		service.putObject(bucket.getName(), object.getKey() + FileName.SEPARATOR, getEmptyInputStream(), getEmptyMetadata());
	}

	@Override
	protected long doGetLastModifiedTime() throws Exception {
		return this.object.getObjectMetadata().getLastModified().getTime();
	}

	@Override
	protected boolean doSetLastModifiedTime(final long modtime) throws Exception {
		// TODO: last modified date will be changed only when content changed
		this.object.getObjectMetadata().setLastModified(new Date(modtime));
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
		String contentType = object.getObjectMetadata().getContentType();
		if (null == contentType) {
			return FileType.FOLDER;
		}
		if ("".equals(object.getKey()) || object.getKey().endsWith("/")) {
			return FileType.FOLDER;
		}
		return FileType.FILE;
	}

	protected String[] XXXXXdoListChildren() throws Exception {
		String path = getS3Key();
		// make sure we add a '/' slash at the end to find children
		if (!"".equals(path)) {
			path = path + "/";
		}
		// S3Object[] children = this.service.listObjects(this.bucket.getName(),
		// path, "/");
		final ListObjectsRequest listObjectRequest = new ListObjectsRequest().withBucketName(this.bucket.getName()).withPrefix(path)
				.withDelimiter("/");
		final ObjectListing objectListing = service.listObjects(listObjectRequest);
		List<String> c = new ArrayList<String>();
		for (final S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
			final String key = objectSummary.getKey();
			if (isImmediateDescendant(path, key)) {
				final String relativePath = getRelativePath(path, key);
				c.add(relativePath);
			}
		}
		return c.toArray(new String[0]);
	}

	protected String[] XXdoListChildren() throws Exception {
		String path = getName().getPath();
		// make sure we add a '/' slash at the end to find children
		if ((!"".equals(path)) && (!path.endsWith(SEPARATOR))) {
			path = path + "/";
		}
		final ListObjectsRequest loReq = new ListObjectsRequest();
		String bucketId = ((S3FileName) getName()).getBucketId();
		loReq.setBucketName(bucketId);
		loReq.setDelimiter("/");
		if (!path.equals("/"))
			loReq.setPrefix(path);
		ObjectListing listing = service.listObjects(loReq);
		final List<S3ObjectSummary> summaries = new ArrayList<S3ObjectSummary>(listing.getObjectSummaries());
		final Set<String> commonPrefixes = new TreeSet<String>(listing.getCommonPrefixes());
		while (listing.isTruncated()) {
			listing = service.listNextBatchOfObjects(listing);
			summaries.addAll(listing.getObjectSummaries());
			commonPrefixes.addAll(listing.getCommonPrefixes());
		}
		List<String> childrenNames = new ArrayList<String>(summaries.size() + commonPrefixes.size());
		// add the prefixes (non-empty subdirs) first
		for (String commonPrefix : commonPrefixes) {
			// strip path from name (leave only base name)
			final String stripPath = commonPrefix.substring(path.length());
			childrenNames.add(stripPath);
		}
		for (S3ObjectSummary summary : summaries) {
			if (!summary.getKey().equals(path)) {
				// strip path from name (leave only base name)
				final String stripPath = summary.getKey().substring(path.length())
						.substring(path.lastIndexOf('/', path.length() - 2));
				childrenNames.add(stripPath);
			}
		}
		return childrenNames.toArray(new String[childrenNames.size()]);
	}

	protected String[] XXXXXXdoListChildren() throws Exception {
		String path = object.getKey();
		// make sure we add a '/' slash at the end to find children
		if ((!"".equals(path)) && (!path.endsWith(SEPARATOR))) {
			path = path + "/";
		}
		List<S3ObjectSummary> children = service.listObjects(bucket.getName(), path).getObjectSummaries();
		List<String> childrenNames = new ArrayList<String>(children.size());
		for (S3ObjectSummary child : children) {
			if (!child.getKey().equals(path)) {
				// strip path from name (leave only base name)
				String cpath = child.getKey();
				String stripPath = cpath.substring(path.length());
				int clast = stripPath.indexOf('/');
				boolean dir = false;
				if (clast != -1) {
					dir = true;
					stripPath = stripPath.substring(0, clast);
				}
				if (dir)
					stripPath += "/";
				if (!childrenNames.contains(stripPath))
					childrenNames.add(stripPath);
			}
		}
		return childrenNames.toArray(new String[childrenNames.size()]);
	}

	@Override
	protected String[] doListChildren() throws Exception {
		String path = object.getKey();
		// make sure we add a '/' slash at the end to find children
		if ((!"".equals(path)) && (!path.endsWith(SEPARATOR))) {
			path = path + "/";
		}
		final ListObjectsRequest loReq = new ListObjectsRequest();
		loReq.setBucketName(((S3FileName) getName()).getBucketId());
		loReq.setDelimiter("/");
		loReq.setPrefix(path);
		ObjectListing listing = service.listObjects(loReq);
		final List<S3ObjectSummary> summaries = new ArrayList<S3ObjectSummary>(listing.getObjectSummaries());
		final Set<String> commonPrefixes = new TreeSet<String>(listing.getCommonPrefixes());
		while (listing.isTruncated()) {
			listing = service.listNextBatchOfObjects(listing);
			summaries.addAll(listing.getObjectSummaries());
			commonPrefixes.addAll(listing.getCommonPrefixes());
		}
		List<String> childrenNames = new ArrayList<String>(summaries.size() + commonPrefixes.size());
		// add the prefixes (non-empty subdirs) first
		for (String commonPrefix : commonPrefixes) {
			// strip path from name (leave only base name)
			final String stripPath = commonPrefix.substring(path.length());
			childrenNames.add(stripPath);
		}
		for (S3ObjectSummary summary : summaries) {
			if (!summary.getKey().equals(path)) {
				// strip path from name (leave only base name)
				final String stripPath = summary.getKey().substring(path.length());
				childrenNames.add(stripPath);
			}
		}
		return childrenNames.toArray(new String[childrenNames.size()]);
	}

	/**
	 * Lists the children of this file. Is only called if {@link #doGetType}
	 * returns {@link FileType#FOLDER}. The return value of this method is
	 * cached, so the implementation can be expensive.<br>
	 * Other than <code>doListChildren</code> you could return FileObject's to
	 * e.g. reinitialize the type of the file.<br>
	 * (Introduced for Webdav: "permission denied on resource" during getType())
	 * 
	 * @return The children of this FileObject.
	 * @throws Exception if an error occurs.
	 */
	@Override
	protected FileObject[] doListChildrenResolved() throws Exception {
		String path = object.getKey();
		// make sure we add a '/' slash at the end to find children
		if ((!"".equals(path)) && (!path.endsWith(SEPARATOR))) {
			path = path + "/";
		}
		final ListObjectsRequest loReq = new ListObjectsRequest();
		loReq.setBucketName(((S3FileName) getName()).getBucketId());
		loReq.setDelimiter("/");
		loReq.setPrefix(path);
		ObjectListing listing = service.listObjects(loReq);
		final List<S3ObjectSummary> summaries = new ArrayList<S3ObjectSummary>(listing.getObjectSummaries());
		final Set<String> commonPrefixes = new TreeSet<String>(listing.getCommonPrefixes());
		while (listing.isTruncated()) {
			listing = service.listNextBatchOfObjects(listing);
			summaries.addAll(listing.getObjectSummaries());
			commonPrefixes.addAll(listing.getCommonPrefixes());
		}
		List<FileObject> resolvedChildren = new ArrayList<FileObject>(summaries.size() + commonPrefixes.size());
		// add the prefixes (non-empty subdirs) first
		for (String commonPrefix : commonPrefixes) {
			// strip path from name (leave only base name)
			String stripPath = commonPrefix.substring(path.length());
			while (stripPath.endsWith("/"))
				stripPath = stripPath.substring(0, stripPath.length() - 1);
			if (!stripPath.equals("")) {
				FileObject childObject = resolveFile(stripPath, (stripPath.equals("/")) ? NameScope.FILE_SYSTEM : NameScope.CHILD);
				if ((childObject instanceof S3FileObject) && !stripPath.equals("/")) {
					resolvedChildren.add(childObject);
				}
			}
		}
		for (S3ObjectSummary summary : summaries) {
			if (!summary.getKey().equals(path)) {
				// strip path from name (leave only base name)
				final String stripPath = summary.getKey().substring(path.length());
				FileObject childObject = resolveFile(stripPath, NameScope.CHILD);
				if (childObject instanceof S3FileObject) {
					S3FileObject s3FileObject = (S3FileObject) childObject;
					ObjectMetadata childMetadata = new ObjectMetadata();
					childMetadata.setContentLength(summary.getSize());
					childMetadata.setContentType(Mimetypes.getInstance().getMimetype(s3FileObject.getName().getBaseName()));
					childMetadata.setLastModified(summary.getLastModified());
					childMetadata.setHeader(Headers.ETAG, summary.getETag());
					// s3FileObject.objectMetadata = childMetadata;
					// s3FileObject.objectKey = summary.getKey();
					// s3FileObject.doAattached = true;
					resolvedChildren.add(s3FileObject);
				}
			}
		}
		return resolvedChildren.toArray(new FileObject[resolvedChildren.size()]);
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
		return this.object.getObjectMetadata().getContentLength();
	}

	// Utility methods
	/**
	 * Download S3 object content and save it in temporary file. Do it only if
	 * object was not already downloaded.
	 */
	private void downloadOnce() throws FileSystemException {
		if (!this.downloaded) {
			final String failedMessage = "Failed to download S3 Object %s. %s";
			String objectPath = ((S3FileName) getName()).getPath();
			try {
				S3Object obj = this.service.getObject(this.bucket.getName(), getS3Key());
				this.logger.info(String.format("Downloading S3 Object: %s", objectPath));
				InputStream is = obj.getObjectContent();
				if (obj.getObjectMetadata().getContentLength() > 0) {
					ReadableByteChannel rbc = Channels.newChannel(is);
					FileChannel cacheFc = getCacheFileChannel();
					cacheFc.transferFrom(rbc, 0, obj.getObjectMetadata().getContentLength());
					cacheFc.close();
					rbc.close();
				} else {
					is.close();
				}
			} catch (AmazonServiceException e) {
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
		String path = ((S3FileName) getName()).getPath();
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
	@SuppressWarnings("resource")
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
	private Owner getS3Owner() throws AmazonServiceException {
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
	private AccessControlList getS3Acl() throws AmazonServiceException {
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
			this.service.setBucketAcl(this.bucket.getName(), s3Acl);
		} else {
			// Before any operations with object it must be attached
			doAttach();
			// Put ACL to S3
			// this.object.setAcl(s3Acl);
			this.service.setObjectAcl(this.bucket.getName(), this.object.getKey(), s3Acl);
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
		} catch (AmazonServiceException e) {
			throw new FileSystemException(e);
		}
		// Get S3 file owner
		Owner owner = s3Acl.getOwner();
		this.fileOwner = owner;
		// Read S3 ACL list and build VFS ACL.
		for (Grant item : s3Acl.getGrants()) {
			// Map enums to jets3t ones
			Permission perm = item.getPermission();
			Acl.Permission[] rights;
			if (perm.equals(Permission.FullControl)) {
				rights = Acl.Permission.values();
			} else if (perm.equals(Permission.Read)) {
				rights = new Acl.Permission[1];
				rights[0] = Acl.Permission.READ;
			} else if (perm.equals(Permission.Write)) {
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
				if (GroupGrantee.AllUsers.equals(grantee)) {
					// Allow rights to GUEST
					myAcl.allow(Acl.Group.EVERYONE, rights);
				} else if (GroupGrantee.AuthenticatedUsers.equals(grantee)) {
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
		Owner owner;
		try {
			owner = getS3Owner();
		} catch (AmazonServiceException e) {
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
				perm = Permission.FullControl;
			} else if (acl.isAllowed(group, Acl.Permission.READ)) {
				perm = Permission.Read;
			} else if (acl.isAllowed(group, Acl.Permission.WRITE)) {
				perm = Permission.Write;
			} else {
				this.logger.error(String.format("Skip unknown set of rights %s", rights.toString()));
				continue;
			}
			// Set grantee
			Grantee grantee;
			if (group.equals(Acl.Group.EVERYONE)) {
				grantee = GroupGrantee.AllUsers;
			} else if (group.equals(Acl.Group.AUTHORIZED)) {
				grantee = GroupGrantee.AuthenticatedUsers;
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
		private AmazonS3Client service;
		private S3Object object;

		public S3OutputStream(OutputStream out, AmazonS3Client service, S3Object object) {
			super(out);
			this.service = service;
			this.object = object;
		}

		@Override
		protected void onClose() throws IOException {
			this.object.getObjectMetadata().setContentType(Mimetypes.getInstance().getMimetype(this.object.getKey()));
			try {
				this.service.putObject(this.object.getBucketName(), this.object.getKey(),
						Channels.newInputStream(getCacheFileChannel()), this.object.getObjectMetadata());
			} catch (AmazonServiceException e) {
				IOException ioe = new IOException();
				ioe.initCause(e);
				throw ioe;
			}
		}
	}

	private ObjectMetadata getEmptyMetadata() {
		ObjectMetadata objectMetadata = new ObjectMetadata();
		objectMetadata.setLastModified(new Date());
		return objectMetadata;
	}

	private InputStream getEmptyInputStream() {
		return new ByteArrayInputStream(new byte[0]);
	}

	private String getS3Key(FileName fileName) {
		String path = ((S3FileName) getName()).getPath();
		if (!"".equals(path)) {
			path = path.substring(1);
		}
		if (fileName.getType().equals(FileType.FOLDER) && !path.endsWith("/")) {
			path += "/";
		}
		return path;
	}
}
