package com.sshtools.vfs.afp;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.auth.StaticUserAuthenticator;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;

import com.sshtools.jafp.common.AFPConstants;

public class Test {
	public static void main(String[] args) throws FileSystemException {
//		VFS.setUriStyle(true);
//		FileObject obj = VFS.getManager().resolveFile("afp://blue");

		FileObject obj;;
		FileSystemOptions opts = new FileSystemOptions();
		AFPFileSystemConfigBuilder.getInstance().setAuthenticationMethods(opts, AFPConstants.UAM_STR_DHX_128);
//		DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, new StaticUserAuthenticator(null, "admin", "Qwerty123?"));
		//DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, new StaticUserAuthenticator(null, "tanktarta", "perissa9000-"));
		DefaultFileSystemConfigBuilder.getInstance().setUserAuthenticator(opts, new StaticUserAuthenticator(null, "tanktarta", "raschid123#"));
		
//		obj = VFS.getManager().resolveFile("afp://blue:1548" ,opts);
		obj = VFS.getManager().resolveFile("afp://fat" ,opts);
		System.err.println("------" + obj.getName().getBaseName() + ", " + obj.getName().getParent());
		for(FileObject c : obj.getChildren()) {
			System.err.println("+++++++" + c.getPublicURIString() + "," + c.getName().getBaseName() + ", " + c.getName().getParent() + ", " + c.getType());
			for(FileObject d : c.getChildren()) {
				System.err.println(">>>>>>>" + d.getPublicURIString() + "," + d.getName().getBaseName() + ", " + d.getName().getParent() + ", " + d.getType());
			}
		}

//		obj = VFS.getManager().resolveFile("afp://blue:1548");
//		for(FileObject c : obj.getChildren()) {
//			System.err.println(c.getPublicURIString());
//		}
		
		
		VFS.getManager().closeFileSystem(obj.getFileSystem());
	}
}
