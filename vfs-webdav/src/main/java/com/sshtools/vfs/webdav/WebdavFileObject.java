package com.sshtools.vfs.webdav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.UriParser;
import org.apache.commons.vfs2.util.MonitorOutputStream;

import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.util.SardineUtil;

public class WebdavFileObject extends AbstractFileObject<WebdavFileSystem> implements FileObject {
	/**
	 * Output stream used to write data to remote file
	 * 
	 * @author ndx
	 *
	 */
	public class WebdavOutputStream extends MonitorOutputStream {

		public WebdavOutputStream() {
			super(new ByteArrayOutputStream());
		}

		/**
		 * On close, the file is written
		 * 
		 * @throws IOException
		 *             on I/O error
		 * @see org.apache.commons.vfs2.util.MonitorOutputStream#onClose()
		 */
		@Override
		protected void onClose() throws IOException {
			sardine.put(getUrl(), ((ByteArrayOutputStream) out).toByteArray());
		}
	}

	private static final String MIME_DIRECTORY = "httpd/unix-directory";
	private Sardine sardine;
	private WebdavFileSystemConfigBuilder builder;
	private String urlCharset;
	private long lastModified = Long.MIN_VALUE;

	public WebdavFileObject(AbstractFileName name, WebdavFileSystem fileSystem, Sardine sardine) {
		super(name, fileSystem);
		this.sardine = sardine;
		builder = (WebdavFileSystemConfigBuilder) WebdavFileSystemConfigBuilder.getInstance();
		this.urlCharset = builder.getUrlCharset(getFileSystem().getFileSystemOptions());
	}

	@Override
	public URLFileName getName() {
		return (URLFileName) super.getName();
	}

	@Override
	protected void doDetach() throws Exception {
		lastModified = Long.MIN_VALUE;
	}

	@Override
	protected FileType doGetType() throws Exception {
		String url = getUrl();
		if (sardine.exists(url)) {
			DavResource serverSide = getServerSide();
			if (serverSide.getContentType().equals(MIME_DIRECTORY))
				return FileType.FOLDER;
			else
				return FileType.FILE;
		} else {
			return FileType.IMAGINARY;
		}
	}

	private DavResource getServerSide() throws IOException {
		return getServerSide(getUrl(), getName().getPath());
	}

	/**
	 * Get server-side resource associated to url
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	private DavResource getServerSide(String url, String path) throws IOException {
		List<DavResource> resources = sardine.list(url);
		for (DavResource res : resources) {
			if (res.getPath().equals(path))
				return res;
			else if (!path.endsWith("/") && res.getPath().equals(path + "/"))
				return res;
		}
		return null;
	}

	@Override
	protected String[] doListChildren() throws Exception {
		// Shouldn't be called for simple files
		if (getType() == FileType.FOLDER) {
			List<DavResource> resources = sardine.list(ensureSlash(getUrl()));
			List<String> children = new LinkedList<String>();

			String thisPath = getName().getPath();
			if (!thisPath.endsWith("/")) {
				thisPath += "/";
			}

			for (DavResource res : resources) {
				String href = UriParser.decode(res.getHref().toString());
				if (!thisPath.equals(href)) {
					children.add(href);
				}
			}
			return children.toArray(new String[children.size()]);
		} else {
			return null;
		}
	}

	@Override
	protected long doGetContentSize() throws Exception {
		return getServerSide().getContentLength();
	}

	@Override
	protected InputStream doGetInputStream() throws Exception {
		return sardine.get(getUrl());
	}

	@Override
	protected void doCreateFolder() throws Exception {
		sardine.createDirectory(ensureSlash(getUrl()));
	}

	@Override
	protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
		return new WebdavOutputStream();
	}

	@Override
	protected void doDelete() throws Exception {
		sardine.delete(getUrl());
	}

	@Override
	protected long doGetLastModifiedTime() throws Exception {
		if (lastModified == Long.MIN_VALUE) {
			return lastModified = getServerSide().getModified().getTime();
		}
		return lastModified;
	}

	@Override
	protected boolean doIsSameFile(FileObject destFile) throws FileSystemException {
		return destFile.getURL().equals(getURL());
	}

	@Override
	protected void doSetAttribute(String attrName, Object value) throws Exception {
		Map<String, String> properties = new HashMap<String, String>(1);
		properties.put(attrName, value.toString());
		sardine.patch(getUrl(), SardineUtil.toQName(properties), null);
	}

	@Override
	protected void doRemoveAttribute(String attrName) throws Exception {
		List<String> properties = new LinkedList<String>();
		properties.add(attrName);
		sardine.patch(getUrl(), (Map<QName, String>) null, SardineUtil.toQName(properties));
	}

	public String getUrl() {
		return urlString(getName());
	}

	private String ensureSlash(String path) {
		return path.endsWith("/") ? path : path + "/";
	}

	private String urlString(URLFileName name) {
		return urlString(name, urlCharset);
	}

	/**
	 * Convert the FileName to an encoded url String.
	 *
	 * @param name
	 *            The FileName.
	 * @param includeUserInfo
	 *            true if user information should be included.
	 * @param urlCharset
	 *            expected charset of urls (may be null)
	 * @return The encoded URL String.
	 */
	static String urlString(URLFileName name, String urlCharset) {
		String scheme = "http";
		if(name.getScheme().equals("webdavs"))
			scheme = "https";
		
		URLFileName newFile = new URLFileName(scheme, name.getHostName(), name.getPort(), name.getDefaultPort(), null,
				null, name.getPath(), name.getType(), name.getQueryString());
		try {
			// TODO - turns out Commons VFS itself does not support this... so I wonder what the point of the option is
			return newFile.getURIEncoded(null);
//			return newFile.getURIEncoded(urlCharset);
		} catch (Exception e) {
			return newFile.getURI();
		}
	}
}
