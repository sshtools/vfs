package com.sshtools.vfs.googledrive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;

import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files.Get;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.User;

public class GDriveFileObject extends AbstractFileObject<GDriveFileSystem> {
	final static Log LOG = LogFactory.getLog(GDriveFileObject.class);
	public final static int DEFAULT_BLOB_INCREMENT = 1024;
	public final static int DEFAULT_BLOB_SIZE = 1024;
	private boolean attached = false;
	private List<File> children;
	private final Drive drive;
	private File file;

	public GDriveFileObject(AbstractFileName fileName, GDriveFileSystem fileSystem, Drive drive) throws FileSystemException {
		super(fileName, fileSystem);
		this.drive = drive;
	}

	@Override
	protected void doAttach() throws IOException {
		if (!attached) {
			file = null;
			children = null;
			GDriveFileObject parentFile = null;
			String path = getName().getPath();
			if (!path.equals("/") && getParent() instanceof GDriveFileObject) {
				parentFile = (GDriveFileObject) getParent();
			}
			/*
			 * If we have a parent, and it is a google file, is this file in
			 * it's children? If so,
			 */
			if(parentFile != null && !parentFile.attached)
				parentFile.doAttach();
			if (parentFile != null && parentFile.children != null) {
				file = findFile(getName().getBaseName());
			}
			/**
			 * The path is for a for, if we did not find it in it's parent, the
			 * resolve the entire path so we can try again to retrieve from the
			 * parent file
			 */
			if (file == null && !path.equals("/")) {
				parentFile = (GDriveFileObject) getFileSystem().resolveFile(getName().getParent());
				if(parentFile != null && !parentFile.attached)
					parentFile.doAttach();
				if (parentFile != null && parentFile.children != null) {
					file = findFile(getName().getBaseName());
				}
				if (file == null) {
					// File is imaginary
					return;
				}
			}
			com.google.api.services.drive.Drive.Files.List request = null;
			if (path.equals("/")) {
				/* Root */
				request = drive.files().list().setQ("'root' in parents");
			} else if (file != null && "application/vnd.google-apps.folder".equals(file.getMimeType())) {
				/* If this is known to be a directory, list the files in it */
				request = drive.files().list().setQ(String.format("'%s' in parents", file.getId()));
			}
			if (request != null) {
				children = new ArrayList<File>();
				do {
					try {
						FileList files = request.execute();
						List<File> newFiles = files.getFiles();
						children.addAll(newFiles);
						request.setPageToken(files.getNextPageToken());
					} catch (IOException e) {
						System.out.println("An error occurred: " + e);
						request.setPageToken(null);
					}
				} while (request.getPageToken() != null && request.getPageToken().length() > 0);
			}
			attached = true;
		}
	}

	@Override
	protected void doCreateFolder() throws URISyntaxException, IOException {
		GDriveFileObject parent = (GDriveFileObject) getParent();
		file = new File();
		file.setMimeType("application/vnd.google-apps.folder");
		file.setName(getName().getBaseName());
		if (parent.file != null)
			file.setParents(Arrays.asList(parent.file.getId()));
		file = drive.files().create(file).execute();
		attached = false;
		parent.children.add(file);
		doAttach();
	}

	@Override
	protected void doDelete() throws Exception {
		drive.files().delete(file.getId()).execute();
	}

	@Override
	protected void doDetach() throws Exception {
		if (this.attached) {
			this.attached = false;
			this.children = null;
		}
	}

	@Override
	protected Map<String, Object> doGetAttributes() throws Exception {
		Map<String, Object> attrs = new HashMap<String, Object>();
		if (file.getAppProperties() != null) {
			for (Map.Entry<String, String> en : file.getAppProperties().entrySet()) {
				attrs.put("app." + en.getKey(), en.getValue());
			}
		}
		attrs.put("capabilities", file.getCapabilities());
		attrs.put("classInfo", file.getClassInfo());
		attrs.put("contentHints", file.getContentHints());
		attrs.put("createdTime", processTime(file.getCreatedTime()));
		attrs.put("description", file.getDescription());
		attrs.put("explicitlyTrashed", file.getExplicitlyTrashed());
		attrs.put("fileExtension", file.getFileExtension());
		attrs.put("folderColorRgb", file.getFolderColorRgb());
		attrs.put("fullFileExtension", file.getFullFileExtension());
		attrs.put("headRevisionId", file.getHeadRevisionId());
		attrs.put("iconLink", file.getIconLink());
		attrs.put("id", file.getId());
		attrs.put("imageMediaMetadata", file.getImageMediaMetadata());
		attrs.put("isAppAuthorized", file.getIsAppAuthorized());
		attrs.put("kind", file.getKind());
		attrs.put("lastModifyingUser", processUser(file.getLastModifyingUser()));
		attrs.put("modifiedByMe", file.getModifiedByMe());
		attrs.put("modifiedByMeTime", processTime(file.getModifiedByMeTime()));
		attrs.put("mimeType", file.getMimeType());
		attrs.put("md5Checksum", file.getMd5Checksum());
		attrs.put("originalFilename", file.getOriginalFilename());
		attrs.put("ownedByMe", file.getOwnedByMe());
		attrs.put("owners", file.getOwners());
		attrs.put("permissions", file.getPermissions());
		if (file.getProperties() != null) {
			for (Map.Entry<String, String> en : file.getProperties().entrySet()) {
				attrs.put("prop." + en.getKey(), en.getValue());
			}
		}
		if (file.getUnknownKeys() != null) {
			for (Map.Entry<String, Object> en : file.getUnknownKeys().entrySet()) {
				attrs.put("uk." + en.getKey(), en.getValue());
			}
		}
		attrs.put("quotaBytesUsed", file.getQuotaBytesUsed());
		attrs.put("shared", file.getShared());
		attrs.put("sharedWithMeTime", processTime(file.getSharedWithMeTime()));
		attrs.put("sharingUser", processUser(file.getSharingUser()));
		attrs.put("size", file.getSize());
		attrs.put("spaces", file.getSpaces());
		attrs.put("starred", file.getStarred());
		attrs.put("thumbnailLink", file.getThumbnailLink());
		attrs.put("trashed", file.getTrashed());
		attrs.put("version", file.getVersion());
		attrs.put("videoMediaMetadata", file.getVideoMediaMetadata());
		attrs.put("viewedByMe", file.getViewedByMe());
		attrs.put("viewedByMeTime", processTime(file.getViewedByMeTime()));
		attrs.put("viewersCanCopyContent", file.getViewersCanCopyContent());
		attrs.put("webContentLink", file.getWebContentLink());
		attrs.put("webViewLink", file.getWebViewLink());
		attrs.put("writersCanShare", file.getWritersCanShare());
		return attrs;
	}

	private String processUser(User user) {
		if (user == null)
			return null;
		String un = user.getDisplayName();
		if (user.getEmailAddress() != null)
			un += " (" + user.getEmailAddress() + ")";
		return un;
	}

	private long processTime(DateTime dateTime) {
		return dateTime == null ? 0 : dateTime.getValue();
	}

	@Override
	protected long doGetContentSize() throws Exception {
		return file == null ? 0 : (file.getSize() == null ? 0 : file.getSize().longValue());
	}

	@Override
	protected InputStream doGetInputStream() throws Exception {
		Get request = drive.files().get(file.getId());
		return request.executeMediaAsInputStream();
	}

	@Override
	protected long doGetLastModifiedTime() throws Exception {
		if (file == null)
			return 0;
		if (file.getModifiedTime() == null)
			return 0;
		return file.getModifiedTime().getValue();
	}

	@Override
	protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
		final GDriveFileObject parent = (GDriveFileObject) getParent();
		final PipedOutputStream pout = new PipedOutputStream();
		final PipedInputStream pin = new PipedInputStream(pout);
		/**
		 * LDP - MimeType can be null to allow google system to determine this. Tika seems to be
		 * removed from the latest dependencies so this seemed the more straight forward approach.
		 */
//		final String mimeType = (GDriveFileSystem) getFileSystem()).getTika().detect(getName().getBaseName());
		final AbstractInputStreamContent in = new InputStreamContent(null, pin);
		if (file == null) {
			file = new File();
//			file.setMimeType(mimeType);
			file.setName(getName().getBaseName());
			if (parent.file != null)
				file.setParents(Arrays.asList(parent.file.getId()));
			new Thread("OutputSteam-" + getName()) {
				public void run() {
					try {
						file = drive.files().create(file, in).execute();
						synchronized (GDriveFileObject.this) {
							attached = false;
							parent.children.add(file);
							doAttach();
						}
					} catch (Exception e) {
						LOG.error("Failed to pipe I/O.", e);
					}
				}
			}.start();
		} else {
			new Thread("OutputSteam-" + getName()) {
				public void run() {
					try {
						synchronized (GDriveFileObject.this) {
							parent.children.remove(file);
						}
						file = drive.files().update(file.getId(), file, in).execute();
						synchronized (GDriveFileObject.this) {
							parent.children.add(file);
							doAttach();
						}
					} catch (Exception e) {
						LOG.error("Failed to pipe I/O.", e);
					}
				}
			}.start();
		}
		return pout;
	}

	@Override
	protected FileType doGetType() throws Exception {
		if (attached) {
			if (file == null || "application/vnd.google-apps.folder".equals(file.getMimeType()))
				return FileType.FOLDER;
			else if (file != null)
				return FileType.FILE;
		}
		return FileType.IMAGINARY;
	}

	@Override
	protected boolean doIsHidden() throws Exception {
		return file.getTrashed() != null && file.getTrashed().booleanValue();
	}

	@Override
	protected String[] doListChildren() throws Exception {
		String[] a = new String[children == null ? 0 : children.size()];
		if (children != null) {
			int i = 0;
			for (File f : children) {
				a[i++] = f.getName();
			}
		}
		return a;
	}

	@Override
	protected void doRemoveAttribute(String attrName) throws Exception {
		throw new FileSystemException("Removal of attributes not supported on this file.");
	}

	@Override
	protected void doRename(FileObject newfile) throws Exception {
		GDriveFileObject parent = (GDriveFileObject) newfile.getParent();
		if (parent.children != null && this.file != null)
			parent.children.remove(file);
		file.setName(newfile.getName().getBaseName());
		file.setParents(Arrays.asList(parent.file.getId()));
		file = drive.files().update(file.getId(), file).execute();
		attached = false;
		GDriveFileObject newParent = (GDriveFileObject) newfile.getParent();
		newParent.children.add(file);
		doAttach();
	}

	@Override
	protected void doSetAttribute(String attrName, Object value) throws Exception {
		throw new UnsupportedOperationException();
	}

	private File findFile(String name) throws FileSystemException {
		GDriveFileObject fo = (GDriveFileObject) getParent();
		for (File f : fo.children) {
			if (f.getName().equals(name)) {
				return f;
			}
		}
		return null;
	}
}
