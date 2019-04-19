package com.sshtools.vfs.s3.provider.s3;

import static org.apache.commons.vfs2.UserAuthenticationData.PASSWORD;
import static org.apache.commons.vfs2.UserAuthenticationData.USERNAME;
import static org.apache.commons.vfs2.util.UserAuthenticatorUtils.getData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemConfigBuilder;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;

public class S3FileSystemConfigBuilder extends FileSystemConfigBuilder {
    private static final S3FileSystemConfigBuilder BUILDER = new S3FileSystemConfigBuilder();

    private static final String SERVER_SIDE_ENCRYPTION = S3FileSystemConfigBuilder.class.getName() + ".SERVER_SIDE_ENCRYPTION";
    private static final String REGION = S3FileSystemConfigBuilder.class.getName() + ".REGION";
    private static final String CLIENT_CONFIGURATION = S3FileSystemConfigBuilder.class.getName() + ".CLIENT_CONFIGURATION";
    private static final String MAX_UPLOAD_THREADS = S3FileSystemConfigBuilder.class.getName() + ".MAX_UPLOAD_THREADS";
    private static final String AWS_CREDENTIALS = S3FileSystemConfigBuilder.class.getName() + ".AWS_CREDENTIALS";
    private static final String AMAZON_S3_CLIENT = S3FileSystemConfigBuilder.class.getName() + ".AMAZON_S3_CLIENT";
    private static final String MAX_LIST_SIZE = S3FileSystemConfigBuilder.class.getName() + ".MAX_LIST_SIZE";
    private static final String AUTO_REGION = S3FileSystemConfigBuilder.class.getName() + ".AUTO_REGION";

    public static final int DEFAULT_MAX_UPLOAD_THREADS = 2;
    public static final long DEFAULT_MAX_LIST_SIZE = 1000;

    private static final Log log = LogFactory.getLog(S3FileSystemConfigBuilder.class);

    /**
     * Auth data types necessary for AWS authentification.
     */
    private final static UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = new UserAuthenticationData.Type[] {
            USERNAME, PASSWORD
    };

    private S3FileSystemConfigBuilder()
    {
        super("s3.");
    }

    public static S3FileSystemConfigBuilder getInstance()
    {
        return BUILDER;
    }

    @Override
    protected Class<? extends FileSystem> getConfigClass() {
        return S3FileSystem.class;
    }

    /**
     * use server-side encryption.
     *
     * @param opts The FileSystemOptions.
     * @param serverSideEncryption true if server-side encryption should be used.
     */
    public void setServerSideEncryption(FileSystemOptions opts, boolean serverSideEncryption)
    {
        setParam(opts, SERVER_SIDE_ENCRYPTION, serverSideEncryption);
    }

    /**
     * @param opts The FileSystemOptions.
     * @return true if server-side encryption is being used.
     * @see #setServerSideEncryption(org.apache.commons.vfs2.FileSystemOptions, boolean)
     */
    public Boolean getServerSideEncryption(FileSystemOptions opts)
    {
        return getBoolean(opts, SERVER_SIDE_ENCRYPTION, false);
    }

    /**
     * @param opts The FileSystemOptions.
     * @param region The S3 region to connect to (if null, then US Standard)
     */
    public void setRegion(FileSystemOptions opts, Regions region) {
        setParam(opts, REGION, region == null ? "" : region.getName());
    }

    /**
     * @param opts The FileSystemOptions.
     * @return The S3 region to connect to (if null, then US Standard)
     */
    public Regions getRegion(FileSystemOptions opts) {
        String r = getString(opts, REGION);
        return (r == null || r.equals("")) ? null : Regions.fromName(r);
    }
    
    /**
     * Get whether attempts should be made to automatically switch regions when 
     * traversing into a bucket in another region.
     * 
     * @param opts file system options
     * @return auto switch region
     */
    public boolean isAutoSwitchRegion(FileSystemOptions opts) {
    	return getBoolean(opts, AUTO_REGION, true);
    }
    
    /**
     * Set whether attempts should be made to automatically switch regions when 
     * traversing into a bucket in another region.
     * 
     * @param opts file system options
     * @param autoSwitch auto switch region
     */
    public void setAutoSwitchRegion(FileSystemOptions opts, boolean autoSwitch) {
    	setParam(opts, AUTO_REGION, autoSwitch);
    }

    /**
     * @param opts The FileSystemOptions.
     * @param clientConfiguration The AWS ClientConfiguration object to
     *                            use when creating the connection.
     */
    public void setClientConfiguration(FileSystemOptions opts, ClientConfiguration clientConfiguration) {
        setParam(opts, CLIENT_CONFIGURATION, clientConfiguration);
    }

    /**
     * @param opts The FileSystemOptions.
     * @return The AWS ClientConfiguration object to use when creating the
     * connection.  If none has been set, a default ClientConfiguration is returend,
     * with the following differences:
     *   1. The maxErrorRetry is 8 instead of the AWS default (3).  This
     *      is generally a better setting to use when operating in a production
     *      environment and means approximately up to 2 minutes of retries for
     *      failed operations.
     */
    public ClientConfiguration getClientConfiguration(FileSystemOptions opts) {
        ClientConfiguration clientConfiguration = (ClientConfiguration) getParam(opts, CLIENT_CONFIGURATION);
        if (clientConfiguration == null) {
            clientConfiguration = new ClientConfiguration();
            clientConfiguration.setMaxErrorRetry(8);
        }
        return clientConfiguration;
    }

    /**
     * Set maximum number of threads to use for a single large (16MB or more) upload
     * @param opts The FileSystemOptions
     * @param maxRetries maximum number of threads to use for a single large (16MB or more) upload
     */
    public void setMaxUploadThreads(FileSystemOptions opts, int maxRetries) {
        setParam(opts, MAX_UPLOAD_THREADS, maxRetries);
    }

    /**
     * Get maximum number of threads to use for a single large (16MB or more) upload
     * 
     * @param opts The FileSystemOptions
     * @return maximum number of threads to use for a single large (16MB or more) upload
     */
    public int getMaxUploadThreads(FileSystemOptions opts) {
        return getInteger(opts, MAX_UPLOAD_THREADS, DEFAULT_MAX_UPLOAD_THREADS);
    }
    
    /**
     * Get the maximum number of results that may be returned in a call to {@link FileObject#getChildren()}.
     * Any more objects than this in a single folder will be silently discarded from the list.
     * 
     * @param opts The FileSystemOptions
     * @return max list size
     */
    public long getMaxListSize(FileSystemOptions opts) {
        return getLong(opts, MAX_LIST_SIZE, DEFAULT_MAX_LIST_SIZE);
    }
    
    /**
     * Get the maximum number of results that may be returned in a call to {@link FileObject#getChildren()}.
     * Any more objects than this in a single folder will be silently discarded from the list.
     * 
     * @param opts The FileSystemOptions
     * @param maxListSize max list size
     */
    public void setMaxListSize(FileSystemOptions opts, long maxListSize) {
        setParam(opts, MAX_LIST_SIZE, maxListSize);
    }

    /**
     * Set predefined AWSCredentials object with access and secret keys for accessing AWS.
     *
     * @param opts opts
     * @param credentials credentials
     */
    public void setAWSCredentials(FileSystemOptions opts, AWSCredentials credentials) {
        setParam(opts, AWS_CREDENTIALS, credentials);
    }

    /**
     * Get predefined AWSCredentials object with access and secret keys for accessing AWS.
     *
     * @param options options
     * @return credentials
     */
    public AWSCredentials getAWSCredentials(FileSystemOptions options) throws FileSystemException {
        AWSCredentials credentials = (AWSCredentials) getParam(options, AWS_CREDENTIALS);

        if (credentials != null) {
            return credentials;
        }

        UserAuthenticationData authData = null;

        try {
            // Read authData from file system options
            authData = UserAuthenticatorUtils.authenticate(options, AUTHENTICATOR_TYPES);

            // Fetch AWS key-id and secret key from authData
            String accessKey = UserAuthenticatorUtils.toString(getData(authData, USERNAME, null));
            String secretKey = UserAuthenticatorUtils.toString(getData(authData, PASSWORD, null));

            if (isEmpty(accessKey) || isEmpty(secretKey)) {
                log.warn("Not able to find access or secret keys. Use empty values");

                return null;
            }

            // Initialize S3 service client.
            return (new BasicAWSCredentials(accessKey, secretKey));
        } finally {
            UserAuthenticatorUtils.cleanup(authData);
        }
    }

    /**
     * In case of many S3FileProviders (useful in multi-threaded environment to eliminate commons-vfs internal locks)
     * you could specify one amazon client for all providers.
     *
     * @param opts opts
     * @param client client
     */
    public void setAmazonS3Client(FileSystemOptions opts, AmazonS3Client client) {
        setParam(opts, AMAZON_S3_CLIENT, client);
    }

    /**
     * Get preinitialized AmazonS3 client.
     *
     * @param opts opts
     * @return client
     */
    public AmazonS3Client getAmazonS3Client(FileSystemOptions opts) {
        return (AmazonS3Client) getParam(opts, AMAZON_S3_CLIENT);
    }

    /**
     * Check for empty string FIXME find the same at Amazon SDK
     *
     * @param s string
     * @return true iff string is null or zero length
     */
    private boolean isEmpty(String s) {
        return ((s == null) || (s.length() == 0));
    }
}
