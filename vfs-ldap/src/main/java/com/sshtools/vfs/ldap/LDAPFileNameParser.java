package com.sshtools.vfs.ldap;

import java.util.ArrayList;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.provider.FileNameParser;
import org.apache.commons.vfs2.provider.GenericURLFileNameParser;

public class LDAPFileNameParser extends GenericURLFileNameParser {
	private final static LDAPFileNameParser instance = new LDAPFileNameParser();

	public LDAPFileNameParser() {
		super(0);
	}

	public static FileNameParser getInstance() {
		return instance;
	}

	public final static Name filenameToDn(FileName fileName) throws InvalidNameException {
		FileName n = fileName;
		List<Rdn> rdns = new ArrayList<>();
		while (n != null) {
			String sn = n.getBaseName();
			if (sn.equals(""))
				break;
			for (String p : sn.split(",")) {
				int idx = p.indexOf('=');
				Object val;
				if (idx == -1) {
					val = sn;
					p= "cn";
				} else {
					val = p.substring(idx + 1);
					p= p.substring(0, idx);
				}
				rdns.add(0, new Rdn(p, val));
			}
			n = n.getParent();
		}
		return new LdapName(rdns);
	}
}
