package com.sshtools.vfs.webdav;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.TrustManager;

import org.apache.commons.vfs2.FileSystem;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;
import org.apache.commons.vfs2.provider.webdav.WebdavFileSystem;
import org.apache.http.cookie.Cookie;

public class WebdavFileSystemConfigBuilder extends DefaultFileSystemConfigBuilder {

	private static final int DEFAULT_MAX_HOST_CONNECTIONS = 5;

    private static final int DEFAULT_MAX_CONNECTIONS = 50;

    private static final String OPTION_NAME_PREEMPTIVE_AUTHENTICATION = "preemptiveAuth";

    private static final String TRUST_MANAGER = "trustManager";

    private static final String HOSTNAME_VERIFIER = "hostnameVerifier";

	private static final String MAX_HOST_CONNECTIONS = "maxHostConnections";

	private static final String MAX_TOTAL_CONNECTIONS = "maxTotalConnections";

	private static final String SOCKET_TIMEOUT = "socketTimeout";

	private static final String CONNECT_TIMEOUT = "connectTimeout";

	private static final WebdavFileSystemConfigBuilder BUILDER = new WebdavFileSystemConfigBuilder();

    private WebdavFileSystemConfigBuilder()
    {
        super();
        // TODO set prefix on superclass
    }

    public static WebdavFileSystemConfigBuilder getInstance()
    {
        return BUILDER;
    }

    /**
     * The user name to be associated with changes to the file.
     * @param opts The FileSystem options
     * @param creatorName The creator name to be associated with the file.
     */
    public void setCreatorName(FileSystemOptions opts, String creatorName)
    {
        setParam(opts, "creatorName", creatorName);
    }

    /**
     * Return the user name to be associated with changes to the file.
     * @param opts The FileSystem options
     * @return The creatorName.
     */
    public String getCreatorName(FileSystemOptions opts)
    {
        return getString(opts, "creatorName");
    }

    /**
     * Whether to use versioning.
     * @param opts The FileSystem options.
     * @param versioning true if versioning should be enabled.
     */
    public void setVersioning(FileSystemOptions opts, boolean versioning)
    {
        setParam(opts, "versioning", Boolean.valueOf(versioning));
    }

    /**
     * The cookies to add to the request.
     * @param opts The FileSystem options.
     * @return true if versioning is enabled.
     */
    public boolean isVersioning(FileSystemOptions opts)
    {
        return getBoolean(opts, "versioning", false);
    }

    /**
     * @return The Webdav FileSystem Class object.
     */
    @Override
    protected Class<? extends FileSystem> getConfigClass()
    {
        return WebdavFileSystem.class;
    }


    /**
     * Set the charset used for url encoding.<br>
     *
     * @param opts The FileSystem options.
     * @param chaset the chaset
     */
    public void setUrlCharset(FileSystemOptions opts, String chaset)
    {
        setParam(opts, "urlCharset", chaset);
    }

    /**
     * Set the charset used for url encoding.<br>
     *
     * @param opts The FileSystem options.
     * @return the chaset
     */
    public String getUrlCharset(FileSystemOptions opts)
    {
        return getString(opts, "urlCharset", "UTF-8");
    }

    /**
     * Set the proxy to use for http connection.<br>
     * You have to set the ProxyPort too if you would like to have the proxy really used.
     *
     * @param opts The FileSystem options.
     * @param proxyHost the host
     * @see #setProxyPort
     */
    public void setProxyHost(FileSystemOptions opts, String proxyHost)
    {
        setParam(opts, "proxyHost", proxyHost);
    }

    /**
     * Set the proxy-port to use for http connection.
     * You have to set the ProxyHost too if you would like to have the proxy really used.
     *
     * @param opts The FileSystem options.
     * @param proxyPort the port
     * @see #setProxyHost
     */
    public void setProxyPort(FileSystemOptions opts, int proxyPort)
    {
        setParam(opts, "proxyPort", new Integer(proxyPort));
    }

    /**
     * Get the proxy to use for http connection.
     * You have to set the ProxyPort too if you would like to have the proxy really used.
     *
     * @param opts The FileSystem options.
     * @return proxyHost
     * @see #setProxyPort
     */
    public String getProxyHost(FileSystemOptions opts)
    {
        return getString(opts, "proxyHost");
    }

    /**
     * Get the proxy-port to use for http the connection.
     * You have to set the ProxyHost too if you would like to have the proxy really used.
     *
     * @param opts The FileSystem options.
     * @return proxyPort: the port number or 0 if it is not set
     * @see #setProxyHost
     */
    public int getProxyPort(FileSystemOptions opts)
    {
        return getInteger(opts, "proxyPort", 0);
    }

    /**
     * Set the proxy authenticator where the system should get the credentials from.
     * @param opts The FileSystem options.
     * @param authenticator The UserAuthenticator.
     */
    public void setProxyAuthenticator(FileSystemOptions opts, UserAuthenticator authenticator)
    {
        setParam(opts, "proxyAuthenticator", authenticator);
    }

    /**
     * Get the proxy authenticator where the system should get the credentials from.
     * @param opts The FileSystem options.
     * @return The UserAuthenticator.
     */
    public UserAuthenticator getProxyAuthenticator(FileSystemOptions opts)
    {
        return (UserAuthenticator) getParam(opts, "proxyAuthenticator");
    }

    /**
     * The cookies to add to the request.
     * @param opts The FileSystem options.
     * @param cookies An array of Cookies.
     */
    public void setCookies(FileSystemOptions opts, Cookie[] cookies)
    {
        setParam(opts, "cookies", cookies);
    }

    /**
     * The cookies to add to the request.
     * @param opts The FileSystem options.
     * @return the Cookie array.
     */
    public Cookie[] getCookies(FileSystemOptions opts)
    {
        return (Cookie[]) getParam(opts, "cookies");
    }

    /**
     * The maximum number of connections allowed.
     * @param opts The FileSystem options.
     * @param maxTotalConnections The maximum number of connections.
     * @since 2.0
     */
    public void setMaxTotalConnections(FileSystemOptions opts, int maxTotalConnections)
    {
        setParam(opts, MAX_TOTAL_CONNECTIONS, new Integer(maxTotalConnections));
    }

    /**
     * Retrieve the maximum number of connections allowed.
     * @param opts The FileSystemOptions.
     * @return The maximum number of connections allowed.
     * @since 2.0
     */
    public int getMaxTotalConnections(FileSystemOptions opts)
    {
        return getInteger(opts, MAX_TOTAL_CONNECTIONS, DEFAULT_MAX_CONNECTIONS);
    }

    /**
     * The maximum number of connections allowed to any host.
     * @param opts The FileSystem options.
     * @param maxHostConnections The maximum number of connections to a host.
     * @since 2.0
     */
    public void setMaxConnectionsPerHost(FileSystemOptions opts, int maxHostConnections)
    {
        setParam(opts, MAX_HOST_CONNECTIONS, new Integer(maxHostConnections));
    }

    /**
     * Retrieve the maximum number of connections allowed per host.
     * @param opts The FileSystemOptions.
     * @return The maximum number of connections allowed per host.
     * @since 2.0
     */
    public int getMaxConnectionsPerHost(FileSystemOptions opts)
    {
        return getInteger(opts, MAX_HOST_CONNECTIONS, DEFAULT_MAX_HOST_CONNECTIONS);
    }

    /**
     * Determines if the FileSystemOptions indicate that preemptive
     * authentication is requested.
     * @param opts The FileSystemOptions.
     * @return true if preemptiveAuth is requested.
     * @since 2.0
     */
    public boolean isPreemptiveAuth(FileSystemOptions opts)
    {
        return getBoolean(opts, OPTION_NAME_PREEMPTIVE_AUTHENTICATION, Boolean.FALSE).booleanValue();
    }

    /**
     * Sets the given value for preemptive HTTP authentication (using BASIC) on the
     * given FileSystemOptions object.  Defaults to false if not set.  It may be
     * appropriate to set to true in cases when the resulting chattiness of the
     * conversation outweighs any architectural desire to use a stronger authentication
     * scheme than basic/preemptive.
     * @param opts The FileSystemOptions.
     * @param preemptiveAuth the desired setting; true=enabled and false=disabled.
     */
    public void setPreemptiveAuth(FileSystemOptions opts, boolean preemptiveAuth)
    {
        setParam(opts, OPTION_NAME_PREEMPTIVE_AUTHENTICATION, Boolean.valueOf(preemptiveAuth));
    }
    
    /**
     * Sets the trust manager that validates the SSL certificate.
     * 
     * @param opts options
     * @param trustManager trust manager
     */
    public void setTrustManager(FileSystemOptions opts, TrustManager trustManager)
    {
    	setParam(opts, TRUST_MANAGER, trustManager);
    }
    
    /**
     * Gets the trust manager that validates the SSL certificate.
     * 
     * @param opts The FileSystemOptions.
     * @return trust manager
     */
    public TrustManager getTrustManager(FileSystemOptions opts)
    {
    	return (TrustManager)getParam(opts, TRUST_MANAGER);
    }
    
    /**
     * Sets the hostname verifier that validates the SSL certificate.
     * 
     * @param opts The FileSystemOptions.
     * @param hostnameVerifier hostname verifier
     */
    public void setHostnameVerifier(FileSystemOptions opts, HostnameVerifier hostnameVerifier)
    {
    	setParam(opts, HOSTNAME_VERIFIER, hostnameVerifier);
    }
    
    /**
     * Gets the hostname verifier that validates the SSL certificate.
     * 
     * @param opts The FileSystemOptions.
     * @return hostname verifier
     */
    public HostnameVerifier getHostnameVerifier(FileSystemOptions opts)
    {
    	return (HostnameVerifier)getParam(opts, HOSTNAME_VERIFIER);
    }

    
    /**
     * Sets the socket timeout.
     * 
     * @param opts The FileSystemOptions.
     * @param socketTimeout socket timeout
     */
    public void setSocketTimeout(FileSystemOptions opts, int socketTimeout)
    {
    	setParam(opts, SOCKET_TIMEOUT, socketTimeout);
    }
    
    /**
     * Gets the socket timeout.
     * 
     * @param opts The FileSystemOptions.
     * @return socketTimeout socket timeout
     */
    public int getSocketTimeout(FileSystemOptions opts)
    {
    	Integer param = (Integer)getParam(opts, SOCKET_TIMEOUT);
		return param == null ? 60000 : param;
    }
    
    /**
     * Sets the connect timeout.
     * 
     * @param opts The FileSystemOptions.
     * @param connectTimeout connect timeout
     */
    public void setConnectTimeout(FileSystemOptions opts, int connectTimeout)
    {
    	setParam(opts, CONNECT_TIMEOUT, connectTimeout);
    }
    
    /**
     * Gets the connect timeout.
     * 
     * @param opts The FileSystemOptions.
     * @return connectTimeout socket timeout
     */
    public int getConnectTimeout(FileSystemOptions opts)
    {
    	Integer param = (Integer)getParam(opts, CONNECT_TIMEOUT);
		return param == null ? 60000 : param;
    }
}
