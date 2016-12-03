package com.sshtools.vfs.ext;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileContentInfoFactory;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FilesCache;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.operations.FileOperationProvider;

public class FilteredFileSystemManager<M extends FileSystemManager> implements FileSystemManager {

	protected M underlying;

	public FilteredFileSystemManager(M underlying) {
		this.underlying = underlying;
	}

	public M getUnderlying() {
		return underlying;
	}

	@Override
	public FileObject getBaseFile() throws FileSystemException {
		return underlying.getBaseFile();
	}

	@Override
	public FileObject resolveFile(String name) throws FileSystemException {
		return underlying.resolveFile(name);
	}

	@Override
	public FileObject resolveFile(String name, FileSystemOptions fileSystemOptions) throws FileSystemException {
		return underlying.resolveFile(name, fileSystemOptions);
	}

	@Override
	public FileObject resolveFile(FileObject baseFile, String name) throws FileSystemException {
		return underlying.resolveFile(baseFile, name);
	}

	@Override
	public FileObject resolveFile(File baseFile, String name) throws FileSystemException {
		return underlying.resolveFile(baseFile, name);
	}

	@Override
	public FileName resolveName(FileName root, String name) throws FileSystemException {
		return underlying.resolveName(root, name);
	}

	@Override
	public FileName resolveName(FileName root, String name, NameScope scope) throws FileSystemException {
		return underlying.resolveName(root, name, scope);
	}

	@Override
	public FileObject toFileObject(File file) throws FileSystemException {
		return underlying.toFileObject(file);
	}

	@Override
	public FileObject createFileSystem(String provider, FileObject file) throws FileSystemException {
		return underlying.createFileSystem(provider, file);
	}

	@Override
	public void closeFileSystem(FileSystem filesystem) {
		underlying.closeFileSystem(filesystem);
	}

	@Override
	public FileObject createFileSystem(FileObject file) throws FileSystemException {
		return underlying.createFileSystem(file);
	}

	@Override
	public FileObject createVirtualFileSystem(String rootUri) throws FileSystemException {
		return underlying.createVirtualFileSystem(rootUri);
	}

	@Override
	public FileObject createVirtualFileSystem(FileObject rootFile) throws FileSystemException {
		return underlying.createVirtualFileSystem(rootFile);
	}

	@Override
	public URLStreamHandlerFactory getURLStreamHandlerFactory() {
		return underlying.getURLStreamHandlerFactory();
	}

	@Override
	public boolean canCreateFileSystem(FileObject file) throws FileSystemException {
		return underlying.canCreateFileSystem(file);
	}

	@Override
	public FilesCache getFilesCache() {
		return underlying.getFilesCache();
	}

	@Override
	public CacheStrategy getCacheStrategy() {
		return underlying.getCacheStrategy();
	}

	@Override
	public Class<?> getFileObjectDecorator() {
		return underlying.getFileObjectDecorator();
	}

	@Override
	public Constructor<?> getFileObjectDecoratorConst() {
		return underlying.getFileObjectDecoratorConst();
	}

	@Override
	public FileContentInfoFactory getFileContentInfoFactory() {
		return underlying.getFileContentInfoFactory();
	}

	@Override
	public boolean hasProvider(String scheme) {
		return underlying.hasProvider(scheme);
	}

	@Override
	public String[] getSchemes() {
		return underlying.getSchemes();
	}

	@Override
	public Collection<Capability> getProviderCapabilities(String scheme) throws FileSystemException {
		return underlying.getProviderCapabilities(scheme);
	}

	@Override
	public void setLogger(Log log) {
		underlying.setLogger(log);
	}

	@Override
	public FileSystemConfigBuilder getFileSystemConfigBuilder(String scheme) throws FileSystemException {
		return underlying.getFileSystemConfigBuilder(scheme);
	}

	@Override
	public FileName resolveURI(String uri) throws FileSystemException {
		return underlying.resolveURI(uri);
	}

	@Override
	public void addOperationProvider(String scheme, FileOperationProvider operationProvider)
			throws FileSystemException {
		underlying.addOperationProvider(scheme, operationProvider);
	}

	@Override
	public void addOperationProvider(String[] schemes, FileOperationProvider operationProvider)
			throws FileSystemException {
		underlying.addOperationProvider(schemes, operationProvider);
	}

	@Override
	public FileOperationProvider[] getOperationProviders(String scheme) throws FileSystemException {
		return underlying.getOperationProviders(scheme);
	}

	@Override
	public FileObject resolveFile(URI uri) throws FileSystemException {
		return underlying.resolveFile(uri);
	}

	@Override
	public FileObject resolveFile(URL url) throws FileSystemException {
		return underlying.resolveFile(url);
	}

}
