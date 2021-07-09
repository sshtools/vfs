package com.sshtools.vfs.ldap;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.VFS;

public class TestLDAP {
	
	static void dump(FileObject r, int indent) throws Exception {
		StringBuilder b = new StringBuilder();
		for(int i = 0 ; i < indent; i++)
			b.append(' ');
		if(r.getType() == FileType.FOLDER)
			System.out.println(String.format("%s%-30s %-10s %8d", b.toString(), r.getName().getBaseName(), r.getType(), 0));
		else
			System.out.println(String.format("%s%-30s %-10s %8d", b.toString(), r.getName().getBaseName(), r.getType(), r.getContent().getSize()));
		if(r.getType() == FileType.FOLDER) {
			for(FileObject c : r.getChildren()) {
				dump(c, indent + 1);
			}
		}
	}
	
	public final static void main(String[] args) throws Exception {
		FileSystemManager vfs = VFS.getManager();
		dump(vfs.resolveFile("ldap://blue:1389/"), 0);
	}
}
