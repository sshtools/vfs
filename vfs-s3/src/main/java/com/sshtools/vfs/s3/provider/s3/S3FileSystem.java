package com.sshtools.vfs.s3.provider.s3;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.CacheStrategy;
import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileSystem;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;

/**
 * An S3 file system.
 *
 * @author Marat Komarov
 * @author Matthias L. Jugel
 * @author Moritz Siuts
 * @author Brett Smith
 */
public class S3FileSystem extends AbstractFileSystem {
	static Log log = LogFactory.getLog(S3FileSystem.class);
	private final AmazonS3 service;
	private boolean shutdownServiceOnClose = false;
	private Regions region;
	private Map<Regions, AmazonS3> regionClients = new HashMap<>();
	private S3FileProvider provider;

	public S3FileSystem(S3FileProvider provider, Regions region, S3FileName fileName, AmazonS3 service,
			FileSystemOptions fileSystemOptions) throws FileSystemException {
		super(fileName, null, fileSystemOptions);
		this.service = service;
		this.region = region;
		this.provider = provider;
	}

	@Override
	protected void addCapabilities(Collection<Capability> caps) {
		caps.addAll(S3FileProvider.capabilities);
	}

	protected Regions getRegion() {
		return region;
	}

	protected <T> T tryRegions(S3FileObject object, Regions region, S3Op<T> callable) {
		Regions or = region;
		boolean autoSwitch = S3FileSystemConfigBuilder.getInstance().isAutoSwitchRegion(getFileSystemOptions());
		int tries = 0;
		for (int i = 0; i < 3; i++) {
			try {
				AmazonS3 srv = getService(region);
				T exec = callable.exec(srv);
				return exec;
			} catch (AmazonS3Exception se) {
				tries++;
				region = processRegionRedirect(se);
				if (!autoSwitch || region == null || or == region)
					throw se;
				else
					object.setRegion(region);
			}
		}
		throw new AmazonS3Exception("Operation failed. Tried " + tries + " regions.");
	}

	protected Regions processRegionRedirect(AmazonS3Exception ex) {
		String find = "The bucket is in this region: ";
		String exStr = ex.getMessage();
		if (exStr != null) {
			int idx = exStr.indexOf(find);
			if (idx != -1) {
				try {
					return Regions.valueOf(exStr.substring(idx + find.length()).split("\\.")[0].toUpperCase().replace('-', '_'));
				} catch (Exception e) {
					log.warn("Unrecognised region.", e);
				}
			}
			find = " is wrong; expecting '";
			idx = exStr.indexOf(find);
			if (idx != -1) {
				try {
					return Regions.valueOf(exStr.substring(idx + find.length()).split("'")[0].toUpperCase().replace('-', '_'));
				} catch (Exception e) {
					log.warn("Unrecognised region.", e);
				}
			}
		}
		return null;
	}

	protected AmazonS3 getService(Regions region) {
		if (Objects.equals(region, this.region)) {
			return service;
		} else if (regionClients.containsKey(region)) {
			AmazonS3 s = regionClients.get(region);
			if (s == null)
				throw new IllegalStateException(String.format("Failed to create region specific client for %s.", region));
			return s;
		} else {
			try {
				AmazonS3 s = provider.getClientForRegion(getRootName(), getFileSystemOptions(), region);
				regionClients.put(region, s);
				return s;
			} catch (FileSystemException e) {
				regionClients.put(region, null);
				throw new IllegalStateException(String.format("Failed to create region specific client for %s.", region), e);
			}
		}
	}

	@Override
	public FileObject resolveFile(FileName name) throws FileSystemException {
		if (name instanceof S3FileName) {
			S3FileName s3 = (S3FileName) name;
			S3FileName rootName = new S3FileName(s3.getScheme(), s3.getHostName(), s3.getUserName(), s3.getPassword(), null,
					FileType.FOLDER);
			if (!rootName.getRootURI().equals(name.getRootURI())) {
				throw new FileSystemException("vfs.provider/mismatched-fs-for-name.error", name, rootName, name.getRootURI());
			}
			/*
			 * Avoid the root check that happens in the super method when
			 * resolving a bucket. This lets us list the root
			 */
			FileObject file = getFileFromCache(name);
			if (file == null) {
				try {
					file = createFile((AbstractFileName) name);
				} catch (final Exception e) {
					throw new FileSystemException("vfs.provider/resolve-file.error", name, e);
				}
				file = decorateFileObject(file);
				putFileToCache(file);
			}
			if (getFileSystemManager().getCacheStrategy().equals(CacheStrategy.ON_RESOLVE)) {
				file.refresh();
			}
			return file;
		}
		return super.resolveFile(name);
	}

	@Override
	protected FileObject createFile(AbstractFileName fileName) throws Exception {
		return new S3FileObject(fileName, this);
	}

	@Override
	protected void doCloseCommunicationLink() {
		if (shutdownServiceOnClose) {
			service.shutdown();
			for (AmazonS3 s : regionClients.values())
				s.shutdown();
		}
	}

	public void setShutdownServiceOnClose(boolean shutdownServiceOnClose) {
		this.shutdownServiceOnClose = shutdownServiceOnClose;
	}
}
