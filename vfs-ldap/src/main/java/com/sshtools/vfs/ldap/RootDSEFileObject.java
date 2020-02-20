package com.sshtools.vfs.ldap;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Name;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.provider.AbstractFileName;
import org.apache.commons.vfs2.provider.AbstractFileObject;
import org.apache.commons.vfs2.provider.UriParser;

public class RootDSEFileObject extends AbstractFileObject<LDAPFileSystem> {
	protected RootDSEFileObject(final AbstractFileName name, final LDAPFileSystem fileSystem) {
		super(name, fileSystem);
	}

	protected FileType doGetType() throws Exception {
		return FileType.FOLDER;
	}

	protected void doCreateFolder() throws Exception {
		throw new UnsupportedOperationException();
	}

	protected long doGetLastModifiedTime() throws Exception {
		throw new UnsupportedOperationException();
	}

	protected boolean doSetLastModifiedTime(final long modtime) throws Exception {
		throw new UnsupportedOperationException();
	}

	protected void doDelete() throws Exception {
		throw new UnsupportedOperationException();
	}

	protected void doRename(FileObject newfile) throws Exception {
		throw new UnsupportedOperationException();
	}

	protected String[] doListChildren() throws Exception {
		LDAPClient client = ((LDAPFileSystem) getFileSystem()).getClient();
		final List<String> children = new ArrayList<String>();
		for (Name baseDn : client.getBaseDns()) {
			children.add(baseDn.toString());
		}
		return UriParser.encode(children.toArray(new String[children.size()]));
	}

	@Override
	protected Map<String, Object> doGetAttributes() throws Exception {
		final Map<String, Object> attributes = new HashMap<String, Object>();
		return attributes;
	}

	protected long doGetContentSize() throws Exception {
		return 0;
	}

	protected InputStream doGetInputStream() throws Exception {
		throw new UnsupportedOperationException();
	}

	protected void doAttach() throws Exception {
		super.doAttach();
	}

	protected void doDetach() throws Exception {
		super.doDetach();
	}

	protected OutputStream doGetOutputStream(boolean bAppend) throws Exception {
		throw new UnsupportedOperationException();
	}

	protected boolean doIsHidden() throws Exception {
		return false;
	}
}
