package com.sshtools.vfs.ldap;

import java.io.Closeable;

import javax.naming.Name;
import javax.naming.directory.DirContext;

public interface LDAPClient extends Closeable {

	DirContext getDirContext();

	Name[] getBaseDns();
}