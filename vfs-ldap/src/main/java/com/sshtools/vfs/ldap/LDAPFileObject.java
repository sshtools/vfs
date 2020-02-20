package com.sshtools.vfs.ldap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.UriParser;

public class LDAPFileObject extends AbstractFileObject<LDAPFileSystem> {
	private final LDAPFileSystem fileSystem;
	private Attributes attrs;
	private boolean file;
	private Name ldapName;
	private byte[] ldifContent;

	protected LDAPFileObject(final AbstractFileName name, final LDAPFileSystem fileSystem) {
		super(name, fileSystem);
		try {
			ldapName = LDAPFileNameParser.filenameToDn(name);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Failed to construct LDAP file name.", e);
		}
		this.fileSystem = fileSystem;
	}

	protected FileType doGetType() throws Exception {
		if (attrs == null) {
			return FileType.IMAGINARY;
		}
		if (file) {
			return FileType.FILE;
		}
		return FileType.FOLDER;
	}

	private void statSelf() throws Exception {
		LDAPClient client = getLDAP();
		DirContext dirctx = client.getDirContext();
		LdapContext ctx = (LdapContext) dirctx.lookup(ldapName);
		attrs = ctx.getAttributes("");
		/*
		 *
		 * TODO perhaps there is a better way of doing this
		 **/
		Attribute ocs = attrs.get("objectClass");
		boolean isFile = false;
		boolean isFolder = false;
		/* Are any of the object classes explicitly file object classes */
		if (ocs != null) {
			for (NamingEnumeration<?> nen = ocs.getAll(); !isFile && nen.hasMoreElements();) {
				Object val = nen.nextElement();
				if (LDAPFileSystemConfigBuilder.getInstance().getFileObjectClasses(getFileSystem().getFileSystemOptions())
						.contains(String.valueOf(val)))
					isFile = true;
			}
			/* Are any of the object classes explicitly folder object classes */
			for (NamingEnumeration<?> nen = ocs.getAll(); !isFile && nen.hasMoreElements();) {
				Object val = nen.nextElement();
				if (LDAPFileSystemConfigBuilder.getInstance().getFileObjectClasses(getFileSystem().getFileSystemOptions())
						.contains(String.valueOf(val)))
					isFile = true;
			}
		}
		/* If is a file, and not explicitly a folder, is a file */
		file = isFile && !isFolder;
	}

	private LDAPClient getLDAP() throws IOException {
		return fileSystem.getClient();
	}

	protected void doCreateFolder() throws Exception {
		getLDAP().getDirContext().createSubcontext(ldapName);
	}

	protected long doGetLastModifiedTime() throws Exception {
		throw new UnsupportedOperationException();
	}

	protected boolean doSetLastModifiedTime(final long modtime) throws Exception {
		throw new UnsupportedOperationException();
	}

	protected void doDelete() throws Exception {
		getLDAP().getDirContext().unbind(ldapName);
	}

	protected void doRename(FileObject newfile) throws Exception {
		throw new UnsupportedOperationException();
	}

	protected String[] doListChildren() throws Exception {
		LDAPClient client = getLDAP();
		final List<String> children = new ArrayList<String>();
		DirContext dirctx = client.getDirContext();
		NamingEnumeration<SearchResult> res = dirctx.search(ldapName, null);
		while (res.hasMoreElements()) {
			SearchResult r = res.next();
			children.add(r.getName());
		}
		return UriParser.encode(children.toArray(new String[children.size()]));
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> doGetAttributes() throws Exception {
		if (attrs == null) {
			throw new FileSystemException("vfs.provider.sftp/get-attributes.error");
		}
		final Map<String, Object> attributes = new HashMap<String, Object>();
		for (NamingEnumeration<Attribute> nen = (NamingEnumeration<Attribute>) attrs.getAll(); nen.hasMoreElements();) {
			Attribute val = nen.nextElement();
			try {
				if (val.size() == 0)
					attributes.put(val.getID(), "");
				else {
					for (NamingEnumeration<?> ne = val.getAll(); ne.hasMoreElements();) {
						Object v = ne.nextElement();
						attributes.put(val.getID(), String.valueOf(v));
					}
				}
			} catch (Exception e) {
				attributes.put(val.getID(), "Error. " + e.getMessage());
			}
		}
		return attributes;
	}

	protected long doGetContentSize() throws Exception {
		if (file)
			return getLDIFContent().length;
		else
			return 0;
	}

	protected InputStream doGetInputStream() throws Exception {
		return new ByteArrayInputStream(getLDIFContent());
	}

	private byte[] getLDIFContent() throws NamingException, UnsupportedEncodingException {
		if (ldifContent == null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			// TODO escaping and stuff? other LDIF format requirements
			for (@SuppressWarnings("unchecked")
			NamingEnumeration<Attribute> ne = (NamingEnumeration<Attribute>) attrs.getAll(); ne.hasMoreElements();) {
				Attribute attr = ne.next();
				for (NamingEnumeration<?> nen = attr.getAll(); nen.hasMoreElements();) {
					Object val = nen.nextElement();
					pw.println(String.format("%s: %s", attr.getID(), val));
				}
			}
			ldifContent = sw.toString().getBytes("UTF-8");
		}
		return ldifContent;
	}

	protected void doAttach() throws Exception {
		super.doAttach();
		statSelf();
	}

	protected void doDetach() throws Exception {
		super.doDetach();
		attrs = null;
	}

	protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
		throw new UnsupportedOperationException();
	}

	protected boolean doIsHidden() throws Exception {
		return false;
	}
}
