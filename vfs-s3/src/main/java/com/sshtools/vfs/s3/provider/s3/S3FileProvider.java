package com.sshtools.vfs.s3.provider.s3;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.vfs2.Capability;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.provider.AbstractOriginatingFileProvider;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

/**
 * An S3 file provider. Create an S3 file system out of an S3 file name. Also
 * defines the capabilities of the file system.
 *
 * @author Marat Komarov
 * @author Matthias L. Jugel
 * @author Moritz Siuts
 */
public class S3FileProvider extends AbstractOriginatingFileProvider {
	public final static Collection<Capability> capabilities = Collections
			.unmodifiableCollection(Arrays.asList(Capability.CREATE, Capability.DELETE, Capability.GET_TYPE,
					Capability.GET_LAST_MODIFIED, Capability.SET_LAST_MODIFIED_FILE, Capability.SET_LAST_MODIFIED_FOLDER,
					Capability.LIST_CHILDREN, Capability.READ_CONTENT, Capability.URI, Capability.WRITE_CONTENT));
	/**
	 * Default options for S3 file system.
	 */
	private static FileSystemOptions defaultOptions = new FileSystemOptions();

	/**
	 * Returns default S3 file system options. Use it to set AWS auth
	 * credentials.
	 * 
	 * @return default S3 file system options
	 */
	public static FileSystemOptions getDefaultFileSystemOptions() {
		return defaultOptions;
	}

	public S3FileProvider() {
		super();
		setFileNameParser(S3FileNameParser.getInstance());
	}

	/**
	 * Create a file system with the S3 root provided.
	 *
	 * @param fileName the S3 file name that defines the root (bucket)
	 * @param fileSystemOptions file system options
	 * @return an S3 file system
	 * @throws FileSystemException if the file system cannot be created
	 */
	@Override
	protected FileSystem doCreateFileSystem(FileName fileName, FileSystemOptions fileSystemOptions) throws FileSystemException {
		final FileSystemOptions fsOptions = (fileSystemOptions != null) ? fileSystemOptions : getDefaultFileSystemOptions();
		final S3FileSystemConfigBuilder config = S3FileSystemConfigBuilder.getInstance();
		AmazonS3 service = config.getAmazonS3Client(fsOptions);
		Regions region = config.getRegion(fsOptions);
		if (region == null)
			region = S3FileObject.DEFAULT_REGION;
		if (service == null) {
			service = getClientForRegion(fileName, fsOptions, region);
		}
		S3FileSystem fileSystem = new S3FileSystem(this, region, (S3FileName) fileName, service, fsOptions);
		if (config.getAmazonS3Client(fsOptions) == null) {
			fileSystem.setShutdownServiceOnClose(true);
		}
		return fileSystem;
	}

	protected AmazonS3 getClientForRegion(FileName fileName, final FileSystemOptions fsOptions, Regions region)
			throws FileSystemException {
		AmazonS3 service;
		final S3FileSystemConfigBuilder config = S3FileSystemConfigBuilder.getInstance();
		AWSCredentials awsCredentials = config.getAWSCredentials(fsOptions);
		ClientConfiguration clientConfiguration = config.getClientConfiguration(fsOptions);
		if (awsCredentials == null) {
			S3FileName s3Name = (S3FileName) fileName;
			if (s3Name.getUserName() != null && s3Name.getPassword() != null) {
				awsCredentials = new BasicAWSCredentials(s3Name.getUserName(), s3Name.getPassword());
			}
		}
		AmazonS3ClientBuilder standard = AmazonS3ClientBuilder.standard();
		if (awsCredentials != null)
			standard.withCredentials(new CP(awsCredentials));
		if (region != null)
			standard.withRegion(region);
		standard.withClientConfiguration(clientConfiguration);
		service = standard.build();
		return service;
	}

	/**
	 * Get the capabilities of the file system provider.
	 *
	 * @return the file system capabilities
	 */
	@Override
	public Collection<Capability> getCapabilities() {
		return capabilities;
	}

	class CP implements AWSCredentialsProvider {
		private AWSCredentials creds;

		CP(AWSCredentials creds) {
			this.creds = creds;
		}

		@Override
		public AWSCredentials getCredentials() {
			return creds;
		}

		@Override
		public void refresh() {
		}
	}
}
