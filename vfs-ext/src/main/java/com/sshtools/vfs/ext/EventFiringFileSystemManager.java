package com.sshtools.vfs.ext;

import java.io.Closeable;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileContentInfo;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.RandomAccessContent;
import org.apache.commons.vfs2.impl.DecoratedFileObject;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.AbstractFileProvider;
import org.apache.commons.vfs2.provider.FileProvider;
import org.apache.commons.vfs2.provider.LocalFileProvider;
import org.apache.commons.vfs2.provider.VfsComponent;
import org.apache.commons.vfs2.util.RandomAccessMode;

public class EventFiringFileSystemManager extends DefaultFileSystemManager
		implements FileObjectEventListener, Closeable {

	private List<FileObjectEventListener> listeners = new ArrayList<FileObjectEventListener>();

	public void addListener(FileObjectEventListener listener) {
		listeners.add(listener);
	}

	public void removeListener(FileObjectEventListener listener) {
		listeners.remove(listener);
	}

	static FileObject wrap(FileObject ob, FileObjectEventListener firer) {
		if (ob == null || ob instanceof MonitoredFile) {
			return ob;
		}
		return new MonitoredFile(firer, ob);
	}

	@Override
	public void addProvider(String urlScheme, FileProvider provider) throws FileSystemException {
		super.addProvider(urlScheme, wrap(provider));
	}

	@Override
	public void addProvider(String[] urlSchemes, FileProvider provider) throws FileSystemException {
		super.addProvider(urlSchemes, wrap(provider));
	}

	@Override
	public FileObject resolveFile(final FileObject baseFile, final String uri,
			final FileSystemOptions fileSystemOptions) throws FileSystemException {
		return wrap(super.resolveFile(baseFile, uri, fileSystemOptions));
	}

	FileObject wrap(FileObject ob) {
		return wrap(ob, this);
	}

	FileProvider wrap(FileProvider provider) {
		return provider instanceof LocalFileProvider ? new LocalMonitoredProvider((LocalFileProvider) provider)
				: new MonitoredProvider(provider);
	}

	class LocalMonitoredProvider extends MonitoredProvider implements LocalFileProvider {

		LocalMonitoredProvider(LocalFileProvider provider) {
			super(provider);
		}

		@Override
		public FileObject findLocalFile(File file) throws FileSystemException {
			return wrap(((LocalFileProvider) provider).findLocalFile(file));
		}

		@Override
		public FileObject findLocalFile(String name) throws FileSystemException {
			return wrap(((LocalFileProvider) provider).findLocalFile(name));
		}

		@Override
		public boolean isAbsoluteLocalName(String name) {
			return ((LocalFileProvider) provider).isAbsoluteLocalName(name);
		}

	}

	static class MonitoredFile extends DecoratedFileObject {

		final FileObjectEventListener firer;

		public MonitoredFile(FileObjectEventListener firer, FileObject file) {
			super(file);
			this.firer = firer;
		}

		@Override
		public void copyFrom(FileObject srcFile, FileSelector selector) throws FileSystemException {
			super.copyFrom(srcFile, selector);
			this.firer.fireUpdatedFile(this);
		}

		@Override
		public void createFile() throws FileSystemException {
			super.createFile();
			this.firer.fireNewFile(this);
		}

		@Override
		public void createFolder() throws FileSystemException {
			super.createFolder();
			this.firer.fireNewFile(this);
		}

		@Override
		public boolean delete() throws FileSystemException {
			boolean delete = super.delete();
			this.firer.fireDeletedFile(this);
			return delete;
		}

		@Override
		public int delete(final FileSelector selector) throws FileSystemException {
			final List<FileObject> deleted = new ArrayList<FileObject>();
			int delete = super.delete(new FileSelector() {

				@Override
				public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
					boolean sel = selector.includeFile(fileInfo);
					if (sel) {
						deleted.add(fileInfo.getFile());
					}
					return sel;
				}

				@Override
				public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
					return selector.traverseDescendents(fileInfo);
				}
			});
			if (delete != deleted.size()) {
				this.firer.fireUpdatedFile(this);
			} else {
				for (FileObject d : deleted) {
					this.firer.fireDeletedFile(d);
				}
			}
			return delete;
		}

		@Override
		public boolean equals(Object obj) {
			return getDecoratedFileObject()
					.equals(obj instanceof MonitoredFile ? ((MonitoredFile) obj).getDecoratedFileObject() : obj);
		}

		@Override
		public FileObject[] findFiles(FileSelector selector) throws FileSystemException {
			FileObject[] f = super.findFiles(selector);
			FileObject[] n = new FileObject[f.length];
			for (int i = 0; i < f.length; i++) {
				n[i] = wrap(f[i]);
			}
			return n;
		}

		@Override
		public void findFiles(FileSelector selector, boolean depthwise, List<FileObject> selected)
				throws FileSystemException {
			ArrayList<FileObject> l = new ArrayList<FileObject>();
			super.findFiles(selector, depthwise, l);
			for (FileObject m : l) {
				selected.add(wrap(m));
			}
		}

		@Override
		public FileObject getChild(String name) throws FileSystemException {
			return wrap(super.getChild(name));
		}

		@Override
		public FileObject[] getChildren() throws FileSystemException {
			FileObject[] f = super.getChildren();
			FileObject[] n = new FileObject[f.length];
			for (int i = 0; i < f.length; i++) {
				n[i] = wrap(f[i]);
			}
			return n;
		}

		@Override
		public FileContent getContent() throws FileSystemException {
			final FileContent content = super.getContent();
			return new FileContent() {

				@Override
				public void close() throws FileSystemException {
					content.close();
				}

				@Override
				public Object getAttribute(String attrName) throws FileSystemException {
					return content.getAttribute(attrName);
				}

				@Override
				public String[] getAttributeNames() throws FileSystemException {
					return content.getAttributeNames();
				}

				@Override
				public Map<String, Object> getAttributes() throws FileSystemException {
					return content.getAttributes();
				}

				@Override
				public Certificate[] getCertificates() throws FileSystemException {
					return content.getCertificates();
				}

				@Override
				public FileContentInfo getContentInfo() throws FileSystemException {
					return content.getContentInfo();
				}

				@Override
				public FileObject getFile() {
					return content.getFile();
				}

				@Override
				public InputStream getInputStream() throws FileSystemException {
					return content.getInputStream();
				}

				@Override
				public long getLastModifiedTime() throws FileSystemException {
					return content.getLastModifiedTime();
				}

				@Override
				public OutputStream getOutputStream() throws FileSystemException {
					final OutputStream contentOut = content.getOutputStream();
					return new FilterOutputStream(contentOut) {
						
						@Override
						public void write(byte[] b, int off, int len) throws IOException {
							contentOut.write(b, off, len);
						}

						@Override
						public void close() throws IOException {
							try {
								super.close();
							} finally {
								firer.fireUpdatedFile(MonitoredFile.this);
							}
						}
					};
				}

				@Override
				public OutputStream getOutputStream(boolean bAppend) throws FileSystemException {
					return new FilterOutputStream(content.getOutputStream(bAppend)) {
						@Override
						public void close() throws IOException {
							try {
								super.close();
							} finally {
								firer.fireUpdatedFile(MonitoredFile.this);
							}
						}
					};
				}

				@Override
				public RandomAccessContent getRandomAccessContent(RandomAccessMode mode) throws FileSystemException {
					return content.getRandomAccessContent(mode);
				}

				@Override
				public long getSize() throws FileSystemException {
					return content.getSize();
				}

				@Override
				public boolean hasAttribute(String attrName) throws FileSystemException {
					return content.hasAttribute(attrName);
				}

				@Override
				public boolean isOpen() {
					return content.isOpen();
				}

				@Override
				public void removeAttribute(String attrName) throws FileSystemException {
					content.removeAttribute(attrName);
				}

				@Override
				public void setAttribute(String attrName, Object value) throws FileSystemException {
					content.setAttribute(attrName, value);
				}

				@Override
				public void setLastModifiedTime(long modTime) throws FileSystemException {
					content.setLastModifiedTime(modTime);
				}

				@Override
				public long write(FileContent output) throws IOException {
					return content.write(output);
				}

				@Override
				public long write(FileObject file) throws IOException {
					return content.write(file);
				}

				@Override
				public long write(OutputStream output) throws IOException {
					return content.write(output);
				}

				@Override
				public long write(OutputStream output, int bufferSize) throws IOException {
					return content.write(output, bufferSize);
				}
			};
		}

		@Override
		public FileObject getParent() throws FileSystemException {

			// TODO ... i don't get why but testing equals on the names or file
			// objects themselves is no good
			if (getName().getURI().equals(getFileSystem().getRootName().getURI())) {
				if (getFileSystem().getParentLayer() != null) {
					return wrap(getFileSystem().getParentLayer().getParent());
				} else {
					return null;
				}
			}
			return wrap(super.getParent());
		}

		@Override
		public int hashCode() {
			return getDecoratedFileObject().hashCode();
		}

		@Override
		public boolean isHidden() throws FileSystemException {
			// TODO make configurable
			return super.isHidden() || getName().getBaseName().endsWith("~");
		}

		@Override
		public void moveTo(FileObject destFile) throws FileSystemException {
			this.firer.fireDeletedFile(this);
			super.moveTo(destFile);
			this.firer.fireNewFile(destFile);
		}

		@Override
		public FileObject resolveFile(String path) throws FileSystemException {
			return wrap(super.resolveFile(path));
		}

		@Override
		public FileObject resolveFile(String name, NameScope scope) throws FileSystemException {
			return wrap(super.resolveFile(name, scope));
		}

		FileObject wrap(FileObject ob) {
			return EventFiringFileSystemManager.wrap(ob, firer);
		}
	}

	class MonitoredProvider extends AbstractFileProvider implements VfsComponent {

		protected FileProvider provider;

		MonitoredProvider(FileProvider provider) {
			this.provider = provider;
		}

		@Override
		public void close() {
			if (provider instanceof VfsComponent) {
				((VfsComponent) provider).close();
			}

		}

		@Override
		public FileObject createFileSystem(String scheme, FileObject file, FileSystemOptions fileSystemOptions)
				throws FileSystemException {
			return wrap(provider.createFileSystem(scheme, file, fileSystemOptions));
		}

		@Override
		public boolean equals(Object obj) {
			return provider.equals(obj);
		}

		@Override
		public FileObject findFile(FileObject baseFile, String uri, FileSystemOptions fileSystemOptions)
				throws FileSystemException {
			return wrap(provider.findFile(baseFile, uri, fileSystemOptions));
		}

		@Override
		public Collection<Capability> getCapabilities() {
			return provider.getCapabilities();
		}

		@Override
		public FileSystemConfigBuilder getConfigBuilder() {
			return provider.getConfigBuilder();
		}

		@Override
		public int hashCode() {
			return provider.hashCode();
		}

		@Override
		public void init() throws FileSystemException {
			if (provider instanceof VfsComponent) {
				((VfsComponent) provider).setLogger(getLogger());
				((VfsComponent) provider).setContext(getContext());
				((VfsComponent) provider).init();
			}
		}

		@Override
		public FileName parseUri(FileName root, String uri) throws FileSystemException {
			return provider.parseUri(root, uri);
		}

	}

	@Override
	public void fireDeletedFile(FileObject f) {
		for (int i = listeners.size() - 1; i >= 0; i--) {
			listeners.get(i).fireDeletedFile(f);
		}
	}

	@Override
	public void fireNewFile(FileObject f) {
		for (int i = listeners.size() - 1; i >= 0; i--) {
			listeners.get(i).fireNewFile(f);
		}
	}

	@Override
	public void fireUpdatedFile(FileObject f) {
		for (int i = listeners.size() - 1; i >= 0; i--) {
			listeners.get(i).fireUpdatedFile(f);
		}
	}

}
