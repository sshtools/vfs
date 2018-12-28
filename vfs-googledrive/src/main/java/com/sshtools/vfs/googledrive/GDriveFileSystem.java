package com.sshtools.vfs.googledrive;

import java.util.Collection;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;
//import org.apache.tika.Tika;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;

public class GDriveFileSystem extends AbstractFileSystem {

	private Drive drive;
	private NetHttpTransport httpTransport;
//	private TiCka tika;

	public GDriveFileSystem(GDriveFileName fileName, Drive drive, FileSystemOptions fileSystemOptions,
			NetHttpTransport httpTransport) throws FileSystemException {
		super(fileName, null, fileSystemOptions);
		this.drive = drive;
		this.httpTransport = httpTransport;
//		tika = new Tika();
	}

//	public Tika getTika() {
//		return tika;
//	}

	@Override
	protected void addCapabilities(Collection<Capability> capabilities) {
		capabilities.addAll(GDriveFileProvider.capabilities);
	}

	@Override
	protected FileObject createFile(AbstractFileName name) throws Exception {
		return new GDriveFileObject(name, this, drive);
	}

	public HttpTransport getTransport() {
		return httpTransport;
	}
}
