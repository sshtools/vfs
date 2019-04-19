package com.sshtools.vfs.s3.provider.s3;

import static com.amazonaws.services.s3.model.ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION;
import static com.sshtools.vfs.s3.operations.Acl.Permission.READ;
import static com.sshtools.vfs.s3.operations.Acl.Permission.WRITE;
import static java.util.Calendar.SECOND;
import static org.apache.commons.vfs2.FileName.SEPARATOR;
import static org.apache.commons.vfs2.NameScope.CHILD;
import static org.apache.commons.vfs2.NameScope.FILE_SYSTEM;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.internal.Mimetypes;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.CanonicalGrantee;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.Grant;
import com.amazonaws.services.s3.model.Grantee;
import com.amazonaws.services.s3.model.GroupGrantee;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.Permission;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.StringUtils;
import com.sshtools.vfs.s3.operations.Acl;
import com.sshtools.vfs.s3.operations.IAclGetter;

/**
 * Implementation of the virtual S3 file system object using the AWS-SDK.<br>
 * Based on Matthias Jugel code. <a href=
 * "http://thinkberg.com/svn/moxo/trunk/modules/vfs.s3/">http://thinkberg.com/svn/moxo/trunk/modules/vfs.s3/</a>
 *
 * @author Marat Komarov
 * @author Matthias L. Jugel
 * @author Moritz Siuts
 * @author Shon Vella
 * @author Brett Smith
 */
public class S3FileObject extends AbstractFileObject<S3FileSystem> {
	public enum ObjectType {
		BUCKET, OBJECT, FOLDER, ROOT
	}

	public final static Regions DEFAULT_REGION = Regions
			.valueOf(System.getProperty("s3.defaultRegion", Regions.DEFAULT_REGION.name()));
	private static final Log logger = LogFactory.getLog(S3FileObject.class);
	private static final String MIMETYPE_JETS3T_DIRECTORY = "application/x-directory";
	private Bucket bucket;
	/**
	 * True when content attached to file
	 */
	// private boolean attached = false;
	/**
	 * Amazon file owner. Used in ACL
	 */
	private Owner fileOwner;
	/** Amazon S3 object */
	private ObjectMetadata objectMetadata;
	private ObjectType objectType = null;
	private Regions region;

	public S3FileObject(AbstractFileName fileName, S3FileSystem fileSystem) throws FileSystemException {
		super(fileName, fileSystem);
	}

	/**
	 * Queries the object if a simple rename to the filename of
	 * <code>newfile</code> is possible.
	 *
	 * @param newfile the new filename
	 * @return true if rename is possible
	 */
	@Override
	public boolean canRenameTo(FileObject newfile) {
		switch (objectType) {
		case ROOT:
		case BUCKET:
			return false;
		default:
			return true;
		}
	}

	/**
	 * Returns access control list for this file.
	 *
	 * VFS interfaces doesn't provide interface to manage permissions. ACL can
	 * be accessed through {@link FileObject#getFileOperations()} Sample:
	 * <code>file.getFileOperations().getOperation(IAclGetter.class)</code>
	 *
	 * @return Current Access control list for a file
	 * @throws FileSystemException on error
	 * 
	 * @see FileObject#getFileOperations()
	 * @see IAclGetter
	 */
	public Acl getAcl() throws FileSystemException {
		Acl myAcl = new Acl();
		AccessControlList s3Acl = getS3Acl();
		// Get S3 file owner
		Owner owner = s3Acl.getOwner();
		fileOwner = owner;
		// Read S3 ACL list and build VFS ACL.
		@SuppressWarnings("deprecation")
		Set<Grant> grants = s3Acl.getGrants();
		for (Grant item : grants) {
			// Map enums to jets3t ones
			Permission perm = item.getPermission();
			Acl.Permission[] rights;
			if (perm.equals(Permission.FullControl)) {
				rights = Acl.Permission.values();
			} else if (perm.equals(Permission.Read)) {
				rights = new Acl.Permission[1];
				rights[0] = READ;
			} else if (perm.equals(Permission.Write)) {
				rights = new Acl.Permission[1];
				rights[0] = WRITE;
			} else {
				// Skip unknown permission
				logger.error(String.format("Skip unknown permission %s", perm));
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
	 * Get direct http url to S3 object.
	 * 
	 * @return the direct http url to S3 object
	 */
	public String getHttpUrl() {
		switch (objectType) {
		case ROOT:
			return "http://s3.amazonaws.com/";
		default:
			StringBuilder sb = new StringBuilder("http://" + getBucketId() + ".s3.amazonaws.com/");
			String key = getS3Key();
			// Determine context. Object or Bucket
			if ("".equals(key)) {
				return sb.toString();
			} else {
				return sb.append(key).toString();
			}
		}
	}

	/**
	 * Get MD5 hash for the file
	 * 
	 * @return md5 hash for file
	 * @throws FileSystemException on error
	 */
	public String getMD5Hash() throws FileSystemException {
		switch (objectType) {
		case ROOT:
		case BUCKET:
			throw new FileSystemException(new IOException("Cannot get MD5 hashfor root or bucket."));
		default:
			String hash = null;
			ObjectMetadata metadata = getObjectMetadata();
			if (metadata != null) {
				hash = metadata.getETag(); // TODO this is something different
											// than
											// mentioned in methodname / javadoc
			}
			return hash;
		}
	}

	public ObjectMetadata getObjectMetadata() throws FileSystemException {
		switch (objectType) {
		case ROOT:
			throw new FileSystemException(new IOException("Cannot get meta data for root."));
		default:
			try {
				return exec(new S3Op<ObjectMetadata>() {
					@Override
					public ObjectMetadata exec(AmazonS3 service) {
						return service.getObjectMetadata(getBucketId(), getS3Key());
					}
				});
			} catch (AmazonServiceException e) {
				throw new FileSystemException(e);
			}
		}
	}

	/**
	 * Get private url with access key and secret key.
	 *
	 * @return the private url
	 * @throws FileSystemException on error
	 */
	public String getPrivateUrl() throws FileSystemException {
		AWSCredentials awsCredentials = S3FileSystemConfigBuilder.getInstance()
				.getAWSCredentials(getFileSystem().getFileSystemOptions());
		if (awsCredentials == null) {
			awsCredentials = AmazonS3ClientHack.extractCredentials(((S3FileSystem) getFileSystem()).getService(getRegion()));
		}
		if (awsCredentials == null) {
			throw new FileSystemException("Not able to build private URL - empty AWS credentials");
		}
		switch (objectType) {
		case ROOT:
			return String.format("s3://%s:%s", awsCredentials.getAWSAccessKeyId(), awsCredentials.getAWSSecretKey());
		case BUCKET:
			return String.format("s3://%s:%s@%s", awsCredentials.getAWSAccessKeyId(), awsCredentials.getAWSSecretKey(),
					getBucketId());
		default:
			return String.format("s3://%s:%s@%s/%s", awsCredentials.getAWSAccessKeyId(), awsCredentials.getAWSSecretKey(),
					getBucketId(), getS3Key());
		}
	}

	/**
	 * Temporary accessible url for object.
	 * 
	 * @param expireInSeconds seconds until expiration
	 * @return temporary accessible url for object
	 * @throws FileSystemException on error
	 */
	public String getSignedUrl(int expireInSeconds) throws FileSystemException {
		switch (objectType) {
		case ROOT:
		case BUCKET:
			throw new FileSystemException(new IOException("Cannot get signed URL for root or bucket."));
		default:
			final Calendar cal = Calendar.getInstance();
			cal.add(SECOND, expireInSeconds);
			try {
				return exec(new S3Op<String>() {
					@Override
					public String exec(AmazonS3 service) {
						return service.generatePresignedUrl(getBucketId(), getS3Key(), cal.getTime()).toString();
					}
				});
			} catch (AmazonServiceException e) {
				throw new FileSystemException(e);
			}
		}
	}

	/**
	 * Returns access control list for this file.
	 *
	 * VFS interfaces doesn't provide interface to manage permissions. ACL can
	 * be accessed through {@link FileObject#getFileOperations()} Sample:
	 * <code>file.getFileOperations().getOperation(IAclGetter.class)</code>
	 *
	 * @param acl the access control list
	 * @throws FileSystemException on error
	 * @see FileObject#getFileOperations()
	 * @see IAclGetter
	 */
	public void setAcl(Acl acl) throws FileSystemException {
		// Create empty S3 ACL list
		AccessControlList s3Acl = new AccessControlList();
		// Get file owner
		Owner owner = getS3Owner();
		s3Acl.setOwner(owner);
		// Iterate over VFS ACL rules and fill S3 ACL list
		Map<Acl.Group, Acl.Permission[]> rules = acl.getRules();
		final Acl.Permission[] allRights = Acl.Permission.values();
		for (Acl.Group group : rules.keySet()) {
			Acl.Permission[] rights = rules.get(group);
			if (rights.length == 0) {
				// Skip empty rights
				continue;
			}
			// Set permission
			Permission perm;
			if (Arrays.equals(rights, allRights)) {
				perm = Permission.FullControl;
			} else if (acl.isAllowed(group, READ)) {
				perm = Permission.Read;
			} else if (acl.isAllowed(group, WRITE)) {
				perm = Permission.Write;
			} else {
				logger.error(String.format("Skip unknown set of rights %s", Arrays.toString(rights)));
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
				logger.error(String.format("Skip unknown group %s", group));
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

	@Override
	protected void doAttach() {
		if (objectType == null) {
			final String bucketId = getBucketId();
			final String candidateKey = getS3Key();
			if (StringUtils.isNullOrEmpty(bucketId)) {
				objectType = ObjectType.ROOT;
			} else {
				if (exec(new S3Op<Boolean>() {
					@Override
					public Boolean exec(AmazonS3 service) {
						return service.doesBucketExistV2(bucketId);
					}
				})) {
					bucket = new Bucket(bucketId);
				}
				if (StringUtils.isNullOrEmpty(candidateKey)) {
					objectType = ObjectType.BUCKET;
				} else {
					/*
					 * Only bother to actually get object details if the bucket
					 * exists
					 */
					if (bucket != null) {
						try {
							// Do we have file with name?
							objectMetadata = exec(new S3Op<ObjectMetadata>() {
								@Override
								public ObjectMetadata exec(AmazonS3 service) {
									return service.getObjectMetadata(getBucketId(), candidateKey);
								}
							});
							objectType = ObjectType.OBJECT;
							return;
						} catch (AmazonS3Exception ase3) {
							String errorCode = ase3.getErrorCode();
							if (errorCode.equalsIgnoreCase("404 not found")) {
								// carry on
							} else
								throw ase3;
						}
						// Do we have folder with that name?
						/*
						 * This just doesn't work. Internally it does a HEAD
						 * request for the folder path, but this ALWAYS seems to
						 * return 404 for folders.
						 * 
						 * Instead. Use the list method on the PARENT, but using
						 * a MARKER of the THIS key. If COMMON PREFIXES is not
						 * empty, then the folder exists. Unfortunately this is
						 * more expensive, but at least it works.
						 */
						/*
						 * try { objectMetadata = exec(new
						 * S3Op<ObjectMetadata>() {
						 * 
						 * @Override public ObjectMetadata exec(AmazonS3
						 * service) { System.out.println("OM " + getBucketId() +
						 * " : " + candidateKey + SEPARATOR); return
						 * service.getObjectMetadata(getBucketId(), candidateKey
						 * + SEPARATOR); } }); objectType = ObjectType.FOLDER;
						 * return; } catch (AmazonS3Exception ase3) { String
						 * errorCode = ase3.getErrorCode(); if
						 * (errorCode.equalsIgnoreCase("404 not found")) { //
						 * carry on } else throw ase3; }
						 */
						final ListObjectsRequest loReq = new ListObjectsRequest();
						loReq.setBucketName(getBucketId());
						loReq.setDelimiter("/");
						S3FileName par = (S3FileName) getName().getParent();
						String p = par.getPath();
						if (!p.equals("/"))
							loReq.setPrefix(p);
						loReq.setMaxKeys(1);
						loReq.setMarker(candidateKey);
						ObjectListing listing = exec(new S3Op<ObjectListing>() {
							@Override
							public ObjectListing exec(AmazonS3 service) {
								return service.listObjects(loReq);
							}
						});
						if (!listing.getCommonPrefixes().isEmpty()) {
							objectType = ObjectType.FOLDER;
						} else
							objectType = ObjectType.OBJECT;
					} else
						objectType = ObjectType.OBJECT;
					objectMetadata = new ObjectMetadata();
				}
			}
		}
	}

	@Override
	protected void doCreateFolder() throws Exception {
		switch (objectType) {
		case ROOT:
			throw new IOException("Cannot create root S3 folder!");
		case BUCKET:
			bucket = exec(new S3Op<Bucket>() {
				@Override
				public Bucket exec(AmazonS3 service) {
					return service.createBucket(getBucketId());
				}
			});
			objectMetadata = null;
			break;
		default:
			String objectKey = getS3Key();
			if (logger.isDebugEnabled()) {
				logger.debug("Create new folder in bucket [" + ((getBucket() != null) ? getBucket().getName() : "null")
						+ "] with key [" + ((objectMetadata != null) ? objectKey : "null") + "]");
			}
			if (objectMetadata == null) {
				return;
			}
			final InputStream input = new ByteArrayInputStream(new byte[0]);
			final ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(0);
			if (getServerSideEncryption()) {
				metadata.setSSEAlgorithm(AES_256_SERVER_SIDE_ENCRYPTION);
			}
			final String dirName = objectKey.endsWith(SEPARATOR) ? objectKey : objectKey + SEPARATOR;
			exec(new S3Op<Void>() {
				@Override
				public Void exec(AmazonS3 service) {
					service.putObject(new PutObjectRequest(getBucketId(), dirName, input, metadata));
					return null;
				}
			});
			objectMetadata = metadata;
			break;
		}
		objectType = ObjectType.FOLDER;
	}

	@Override
	protected void doDelete() throws Exception {
		switch (objectType) {
		case ROOT:
			throw new IOException("Cannot delete all of S3!");
		case BUCKET:
			exec(new S3Op<Void>() {
				@Override
				public Void exec(AmazonS3 service) {
					service.deleteBucket(getBucketId());
					return null;
				}
			});
			break;
		default:
			exec(new S3Op<Void>() {
				@Override
				public Void exec(AmazonS3 service) {
					String k = getS3Key();
					try {
						if (getType() == FileType.FOLDER && !k.endsWith(SEPARATOR))
							k += SEPARATOR;
					} catch (FileSystemException e) {
						throw new AmazonClientException("Could not determine file type for deletion.", e);
					}
					service.deleteObject(getBucketId(), k);
					return null;
				}
			});
			break;
		}
	}

	@Override
	protected void doDetach() throws Exception {
		if (objectType != null) {
			if (logger.isDebugEnabled())
				logger.debug("Detach from S3 Object: " + getS3Key());
			objectMetadata = null;
			objectType = null;
			bucket = null;
		}
	}

	@Override
	protected long doGetContentSize() throws Exception {
		switch (objectType) {
		case ROOT:
		case BUCKET:
			return 0;
		default:
			return objectMetadata.getContentLength();
		}
	}

	@Override
	protected InputStream doGetInputStream() throws Exception {
		switch (objectType) {
		case ROOT:
		case BUCKET:
			throw new IOException("Cannot open root or buckets.");
		default:
			final String objectPath = getName().getPath();
			S3Object obj = null;
			try {
				obj = exec(new S3Op<S3Object>() {
					@Override
					public S3Object exec(AmazonS3 service) {
						return service.getObject(getBucketId(), getS3Key());
					}
				});
				logger.info(String.format("Downloading S3 Object: %s", objectPath));
				if (obj.getObjectMetadata().getContentLength() > 0) {
					return new S3InputStream(obj);
				} else {
					return new S3InputStream();
				}
			} catch (AmazonServiceException e) {
				final String failedMessage = "Failed to download S3 Object %s. %s";
				throw new FileSystemException(String.format(failedMessage, objectPath, e.getMessage()), e);
			}
		}
	}

	@Override
	protected long doGetLastModifiedTime() throws Exception {
		switch (objectType) {
		case ROOT:
			// Probably true ;)
			return System.currentTimeMillis();
		case BUCKET:
			return getBucket().getCreationDate().getTime();
		default:
			return objectMetadata.getLastModified().getTime();
		}
	}

	@Override
	protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
		switch (objectType) {
		case ROOT:
		case BUCKET:
			throw new IOException("Cannot open root or buckets.");
		default:
			return exec(new S3Op<OutputStream>() {
				@Override
				public OutputStream exec(AmazonS3 service) {
					return new S3OutputStream(S3FileObject.this, getBucketId(), getName().getPath());
				}
			});
		}
	}

	@Override
	protected FileType doGetType() throws Exception {
		switch (objectType) {
		case BUCKET:
		case ROOT:
		case FOLDER:
			return FileType.FOLDER;
		default:
			if (isDirectoryPlaceholder()) {
				return FileType.FOLDER;
			}
			if (objectMetadata == null || objectMetadata.getContentType() == null) {
				return FileType.IMAGINARY;
			}
			return FileType.FILE;
		}
	}

	@Override
	protected String[] doListChildren() throws Exception {
		switch (objectType) {
		case ROOT:
			List<String> children = new ArrayList<>();
			for (Bucket bucket : exec(new S3Op<List<Bucket>>() {
				@Override
				public List<Bucket> exec(AmazonS3 service) {
					return service.listBuckets();
				}
			})) {
				children.add(bucket.getName());
				if (children.size() >= S3FileSystemConfigBuilder.getInstance()
						.getMaxListSize(getFileSystem().getFileSystemOptions())) {
					break;
				}
			}
			return children.toArray(new String[0]);
		case OBJECT:
			throw new IOException("Not a folder.");
		default:
			String path = getS3Key();
			// make sure we add a '/' slash at the end to find children
			if ((!"".equals(path)) && (!path.endsWith(SEPARATOR))) {
				path = path + "/";
			}
			final ListObjectsRequest loReq = new ListObjectsRequest();
			loReq.setBucketName(getBucket().getName());
			loReq.setDelimiter("/");
			loReq.setPrefix(path);
			final List<S3ObjectSummary> summaries = new ArrayList<S3ObjectSummary>();
			final Set<String> commonPrefixes = new TreeSet<String>();
			list(loReq, summaries, commonPrefixes);
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
		switch (objectType) {
		case ROOT:
			List<FileObject> children = new ArrayList<>();
			for (Bucket bucket : exec(new S3Op<List<Bucket>>() {
				@Override
				public List<Bucket> exec(AmazonS3 service) {
					return service.listBuckets();
				}
			})) {
				String name = bucket.getName();
				FileObject resolveFile = resolveFile(name);
				((S3FileObject) resolveFile).bucket = bucket;
				((S3FileObject) resolveFile).objectType = ObjectType.BUCKET;
				children.add(resolveFile);
			}
			;
			return children.toArray(new FileObject[0]);
		case OBJECT:
			throw new IOException("Not a folder.");
		default:
			String path = getS3Key();
			// make sure we add a '/' slash at the end to find children
			if ((!"".equals(path)) && (!path.endsWith(SEPARATOR))) {
				path = path + "/";
			}
			final ListObjectsRequest loReq = new ListObjectsRequest();
			loReq.setBucketName(getBucket().getName());
			loReq.setDelimiter("/");
			loReq.setPrefix(path);
			final List<S3ObjectSummary> summaries = new ArrayList<S3ObjectSummary>();
			final Set<String> commonPrefixes = new TreeSet<String>();
			list(loReq, summaries, commonPrefixes);
			List<FileObject> resolvedChildren = new ArrayList<FileObject>(summaries.size() + commonPrefixes.size());
			// add the prefixes (non-empty subdirs) first
			for (String commonPrefix : commonPrefixes) {
				// strip path from name (leave only base name)
				String stripPath = commonPrefix.substring(path.length());
				FileObject childObject = resolveFile(stripPath, (stripPath.equals("/")) ? FILE_SYSTEM : CHILD);
				if ((childObject instanceof S3FileObject) && !stripPath.equals("/")) {
					((S3FileObject) childObject).objectType = ObjectType.FOLDER;
					resolvedChildren.add(childObject);
				}
			}
			for (S3ObjectSummary summary : summaries) {
				if (!summary.getKey().equals(path)) {
					// strip path from name (leave only base name)
					final String stripPath = summary.getKey().substring(path.length());
					FileObject childObject = resolveFile(stripPath, CHILD);
					if (childObject instanceof S3FileObject) {
						S3FileObject s3FileObject = (S3FileObject) childObject;
						ObjectMetadata childMetadata = new ObjectMetadata();
						childMetadata.setContentLength(summary.getSize());
						childMetadata.setContentType(Mimetypes.getInstance().getMimetype(s3FileObject.getName().getBaseName()));
						childMetadata.setLastModified(summary.getLastModified());
						childMetadata.setHeader(Headers.ETAG, summary.getETag());
						s3FileObject.objectMetadata = childMetadata;
						s3FileObject.objectType = ObjectType.OBJECT;
						resolvedChildren.add(s3FileObject);
					}
				}
			}
			return resolvedChildren.toArray(new FileObject[resolvedChildren.size()]);
		}
	}

	@Override
	protected void doRename(FileObject newFile) throws Exception {
		switch (objectType) {
		case ROOT:
		case BUCKET:
			throw new IOException("Cannot rename root or buckets.");
		default:
			if (newFile.getName() instanceof S3FileName) {
				final S3FileName name = (S3FileName) newFile.getName();
				final String okey = getS3Key() + (newFile.isFolder() ? SEPARATOR : "");
				if (StringUtils.isNullOrEmpty(name.getHostName()))
					throw new IOException("Cannot rename to the root of S3.");
				else if (StringUtils.isNullOrEmpty(name.getPath()))
					throw new IOException("Cannot rename bucket.");
				else {
					final String s3Key = getS3Key(name) + (newFile.isFolder() ? "/" : "");
					if (newFile.exists())
						throw new IOException("Target exists.");
					if (isFolder()) {
						final ListObjectsRequest loReq = new ListObjectsRequest();
						loReq.setBucketName(getBucket().getName());
						loReq.setMarker(okey);
						exec(new S3Op<Void>() {
							@Override
							public Void exec(AmazonS3 service) {
								ObjectListing listing = null;
								while (listing == null || listing.isTruncated()) {
									if (listing == null)
										listing = service.listObjects(loReq);
									else
										listing = service.listNextBatchOfObjects(listing);
									for (final S3ObjectSummary l : listing.getObjectSummaries()) {
										if (!l.getKey().startsWith(okey))
											break;
										S3FileObject.this.exec(new S3Op<CopyObjectResult>() {
											@Override
											public CopyObjectResult exec(AmazonS3 service) {
												String lk = s3Key + l.getKey().substring(getName().getBaseName().length());
												return service.copyObject(getBucketId(), l.getKey(), name.getHostName(), lk);
											}
										});
									}
								}
								return null;
							}
						});
					} else {
						exec(new S3Op<CopyObjectResult>() {
							@Override
							public CopyObjectResult exec(AmazonS3 service) {
								return service.copyObject(getBucketId(), okey, name.getHostName(), s3Key);
							}
						});
					}
					deleteAll();
				}
			} else {
				/* Copying across file systems, using fallback method */
				newFile.copyFrom(this, new AllFileSelector());
				deleteAll();
			}
			break;
		}
	}

	@Override
	protected boolean doSetLastModifiedTime(final long modtime) throws Exception {
		switch (objectType) {
		case ROOT:
		case BUCKET:
			return false;
		default:
			long oldModified = objectMetadata.getLastModified().getTime();
			boolean differentModifiedTime = oldModified != modtime;
			if (differentModifiedTime) {
				objectMetadata.setLastModified(new Date(modtime));
			}
			return differentModifiedTime;
		}
	}

	/* Amazon S3 bucket */
	protected Bucket getBucket() throws IOException {
		if (bucket == null) {
			final String bucketId = getBucketId();
			if (StringUtils.isNullOrEmpty(bucketId)) {
				throw new IOException("Cannot get bucket for S3 root.");
			} else {
				if (exec(new S3Op<Boolean>() {
					@Override
					public Boolean exec(AmazonS3 service) {
						return service.doesBucketExistV2(bucketId);
					}
				})) {
					bucket = new Bucket(bucketId);
				} else
					throw new FileNotFoundException(String.format("No bucket named %s", bucketId));
			}
		}
		return bucket;
	}

	protected void setRegion(Regions region) {
		this.region = region;
	}

	<T> T exec(S3Op<T> op) {
		return ((S3FileSystem) getFileSystem()).tryRegions(this, getRegion(), op);
	}

	private String getBucketId() {
		return ((S3FileName) getName()).getHostName();
	}

	private Regions getRegion() {
		if (region != null)
			return region;
		else {
			try {
				if (getParent() == null || !(getParent() instanceof S3FileObject)) {
					Regions r = ((S3FileSystem) getFileSystem()).getRegion();
					if (r == null)
						return DEFAULT_REGION;
					else
						return r;
				} else
					return ((S3FileObject) getParent()).getRegion();
			} catch (FileSystemException fse) {
				throw new AmazonClientException("Could not find region to use.", fse);
			}
		}
	}

	/**
	 * Get S3 ACL list
	 * 
	 * @return acl list
	 */
	private AccessControlList getS3Acl() throws FileSystemException {
		switch (objectType) {
		case ROOT:
			throw new FileSystemException(new IOException("Cannot get ACL on S3 root!"));
		case BUCKET:
			return exec(new S3Op<AccessControlList>() {
				@Override
				public AccessControlList exec(AmazonS3 service) {
					return service.getBucketAcl(getBucketId());
				}
			});
		default:
			return exec(new S3Op<AccessControlList>() {
				@Override
				public AccessControlList exec(AmazonS3 service) {
					return service.getObjectAcl(getBucketId(), getS3Key());
				}
			});
		}
	}

	/**
	 * Create an S3 key from a commons-vfs path. This simply strips the slash
	 * from the beginning if it exists.
	 *
	 * @return the S3 object key
	 */
	private String getS3Key() {
		return getS3Key(getName());
	}

	private String getS3Key(FileName fileName) {
		String path = fileName.getPath();
		if (StringUtils.isNullOrEmpty(path)) {
			return path;
		} else {
			return path.substring(1);
		}
	}

	// ACL extension methods
	/**
	 * Returns S3 file owner. Loads it from S3 if needed.
	 * 
	 * @throws FileSystemException
	 */
	private Owner getS3Owner() throws FileSystemException {
		if (fileOwner == null) {
			AccessControlList s3Acl = getS3Acl();
			fileOwner = s3Acl.getOwner();
		}
		return fileOwner;
	}

	private boolean getServerSideEncryption() {
		return S3FileSystemConfigBuilder.getInstance().getServerSideEncryption(getFileSystem().getFileSystemOptions());
	}

	/**
	 * Same as in Jets3t library, to be compatible.
	 */
	private boolean isDirectoryPlaceholder() {
		String objectKey = getS3Key();
		// Recognize "standard" directory place-holder indications used by
		// Amazon's AWS Console and Panic's Transmit.
		if (objectMetadata != null) {
			if (objectKey.endsWith("/") && objectMetadata.getContentLength() == 0) {
				return true;
			}
			// Recognize s3sync.rb directory placeholders by MD5/ETag value.
			if ("d66759af42f282e1ba19144df2d405d0".equals(objectMetadata.getETag())) {
				return true;
			}
			// Recognize place-holder objects created by the Google Storage
			// console
			// or S3 Organizer Firefox extension.
			if (objectKey != null && objectKey.endsWith("_$folder$") && (objectMetadata.getContentLength() == 0)) {
				return true;
			}
			// Recognize legacy JetS3t directory place-holder objects, only
			// gives
			// accurate results if an object's metadata is populated.
			if (objectMetadata.getContentLength() == 0 && MIMETYPE_JETS3T_DIRECTORY.equals(objectMetadata.getContentType())) {
				return true;
			}
		}
		return false;
	}

	private void list(final ListObjectsRequest loReq, final List<S3ObjectSummary> summaries, final Set<String> commonPrefixes) {
		exec(new S3Op<Void>() {
			@Override
			public Void exec(AmazonS3 service) {
				ObjectListing listing = null;
				long max = S3FileSystemConfigBuilder.getInstance().getMaxListSize(getFileSystem().getFileSystemOptions());
				while (listing == null || listing.isTruncated()) {
					if (listing == null)
						listing = service.listObjects(loReq);
					else
						listing = service.listNextBatchOfObjects(listing);
					summaries.addAll(listing.getObjectSummaries());
					commonPrefixes.addAll(listing.getCommonPrefixes());
					if (summaries.size() + commonPrefixes.size() >= max) {
						break;
					}
				}
				while (summaries.size() + commonPrefixes.size() > max) {
					summaries.remove(0);
				}
				for (Iterator<String> it = commonPrefixes.iterator(); it.hasNext();) {
					it.next();
					if (summaries.size() + commonPrefixes.size() > max)
						it.remove();
				}
				return null;
			}
		});
	}

	/**
	 * Put S3 ACL list
	 * 
	 * @param s3Acl acl list
	 */
	private void putS3Acl(final AccessControlList s3Acl) throws IOException {
		switch (objectType) {
		case ROOT:
			throw new IOException("Cannot set ACL on S3 root!");
		case BUCKET:
			exec(new S3Op<Void>() {
				@Override
				public Void exec(AmazonS3 service) {
					service.setBucketAcl(getBucketId(), s3Acl);
					return null;
				}
			});
			break;
		default:
			// Before any operations with object it must be attached
			doAttach();
			// Put ACL to S3
			exec(new S3Op<Void>() {
				@Override
				public Void exec(AmazonS3 service) {
					service.setObjectAcl(getBucketId(), getS3Key(), s3Acl);
					return null;
				}
			});
			break;
		}
	}
}
