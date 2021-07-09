package com.sshtools.vfs.ldap;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;
import javax.net.SocketFactory;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.provider.GenericFileName;
import org.apache.commons.vfs2.util.UserAuthenticatorUtils;

public class DefaultLDAPClient implements LDAPClient {
	public static final UserAuthenticationData.Type[] AUTHENTICATOR_TYPES = new UserAuthenticationData.Type[] {
			UserAuthenticationData.USERNAME, UserAuthenticationData.PASSWORD };
	private static final String LDAP_SOCKET_FACTORY = "java.naming.ldap.factory.socket";
	public static final String WILDCARD_SEARCH = "*";
	public static final String OBJECT_CLASS_ATTRIBUTE = "objectClass";
	private Hashtable<String, String> env = new Hashtable<String, String>();
	private InitialDirContext initDirContext;
	private GenericFileName rootName;
	private Name[] baseDns;

	public DefaultLDAPClient(GenericFileName rootName, FileSystemOptions fileSystemOptions)
			throws NamingException, URISyntaxException {
		this.rootName = rootName;
		URI uri = new URI(rootName.getFriendlyURI());
		String ldapUrl = uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
		if (uri.getPath() != null && !uri.getPath().equals("") && !uri.getPath().equals("/"))
			ldapUrl += "/" + uri.getPath();
		env.put(Context.PROVIDER_URL, ldapUrl);
		if (uri.getUserInfo() != null) {
			UserAuthenticationData authData = UserAuthenticatorUtils.authenticate(fileSystemOptions, AUTHENTICATOR_TYPES);
			String username = UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
					UserAuthenticationData.USERNAME, UserAuthenticatorUtils.toChar(rootName.getUserName())));
			String password = UserAuthenticatorUtils.toString(UserAuthenticatorUtils.getData(authData,
					UserAuthenticationData.PASSWORD, UserAuthenticatorUtils.toChar(rootName.getPassword())));
			if (username != null)
				env.put(Context.SECURITY_PRINCIPAL, username);
			if (password != null)
				env.put(Context.SECURITY_CREDENTIALS, password);
		} else {
			env.put(Context.SECURITY_AUTHENTICATION, "none");
		}
		SocketFactory fact = LDAPFileSystemConfigBuilder.getInstance().getSocketFactory(fileSystemOptions);
		if (fact != null) {
			env.put(LDAP_SOCKET_FACTORY, ThreadLocalSocketFactory.class.getName());
			ThreadLocalSocketFactory.set(fact);
		}
		try {
			configureSocket(env);
			initDirContext = new InitialDirContext(env);
			// lookupContext(configuration.getBaseDn());
			Attributes rootAttrs = initDirContext.getAttributes("", new String[] { "namingcontexts" });
			if (rootAttrs == null)
				throw new NamingException("Could not find namingcontexts attributes");
			Attribute nc = rootAttrs.get("namingcontexts");
			List<Name> bdn = new ArrayList<Name>();
			for (NamingEnumeration<?> ne = nc.getAll(); ne.hasMoreElements();) {
				Object v = ne.nextElement();
				bdn.add(new LdapName(v.toString()));
			}
			baseDns = bdn.toArray(new Name[0]);
		} finally {
			if (fact != null) {
				ThreadLocalSocketFactory.remove();
			}
		}
	}

	public FileName getRootFileName() {
		return rootName;
	}

	public Name[] getBaseDns() {
		return baseDns;
	}

	@Override
	public void close() throws IOException {
		try {
			initDirContext.close();
		} catch (NamingException e) {
			throw new IOException("Failed to close.", e);
		}
	}

	@Override
	public DirContext getDirContext() {
		return initDirContext;
	}

	private void configureSocket(Hashtable<String, String> env) {
		env.put("com.sun.jndi.ldap.connect.pool", "false");
		// env.put("com.sun.jndi.ldap.connect.pool.debug", "all");
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		// env.put("com.sun.jndi.ldap.connect.pool.protocol", "plain ssl");
	}

	public SearchControls getSearchControls() {
		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		return searchControls;
	}
	// public <T> Iterator<T> search(Filter filter, ResultMapper<T>
	// resultMapper, SearchControls searchControls)
	// throws NamingException, IOException {
	// return search(baseDn, filter, resultMapper, searchControls);
	// }
	//
	// public <T> Iterator<T> search(final Name baseDN, final Filter filter,
	// final ResultMapper<T> resultMapper,
	// final SearchControls searchControls) throws NamingException, IOException
	// {
	// return new SearchResultsIterator<T>(Arrays.asList(baseDN), filter,
	// searchControls, resultMapper, initDirContext, fileSystemOptions);
	// }

	public LdapContext lookupContext(final Name dn) throws NamingException, IOException {
		return (LdapContext) initDirContext.lookup(dn);
	}

	public final Filter buildObjectClassFilter(String objectClass, String principalNameFilterAttribute, String principalName) {
		And filter = new And();
		filter.add(new Eq("objectClass", objectClass));
		filter.add(new Eq(principalNameFilterAttribute, principalName));
		return filter;
	}

	public interface ResultMapper<T> {
		public T apply(SearchResult result) throws NamingException, IOException;

		public boolean isApplyFilters();
	}
}
