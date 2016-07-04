package com.sshtools.vfs.azure;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import org.apache.commons.vfs2.AllFileSelector;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.UserAuthenticationData;
import org.apache.commons.vfs2.UserAuthenticator;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.UserAuthenticationData.Type;
import org.apache.commons.vfs2.impl.DefaultFileSystemConfigBuilder;

public class Test {

	public static void main(String[] args) throws Exception {
		FileSystemOptions opts = new FileSystemOptions();
		DefaultFileSystemConfigBuilder build = new DefaultFileSystemConfigBuilder();
		build.setUserAuthenticator(opts, new UserAuthenticator() {

			@Override
			public UserAuthenticationData requestAuthentication(Type[] types) {
				UserAuthenticationData d = new UserAuthenticationData();
				;
				for (Type t : types) {
					if (t.equals(UserAuthenticationData.USERNAME)) {
						d.setData(t, "hypersocket".toCharArray());
					} else
						if (t.equals(UserAuthenticationData.PASSWORD)) {
						d.setData(t,
								"NY773wol762zVCu75+lpFGkmVtSbPc8rJWY1whYRJE7zcUkwhMQdcMGqx+dpqviQRPVsC7PUrTlh5kTkKBXLMw=="
										.toCharArray());
					}
				}
				return d;
			}
		});
		

//		FileObject obj = VFS.getManager().resolveFile("azure://newcontainer/newsubc/", opts);
//		 dumpFile(obj, 0);
		
		
		FileObject obj = VFS.getManager().resolveFile("azure://hypersocket@azure/", opts);
//		FileObject obj = VFS.getManager().resolveFile("azure://", opts);
//		 dumpFile(obj, 0);

		FileObject newC = obj.resolveFile("newcontainer");
		newC.createFolder();
//		dumpFile(newC, 0);

		FileObject newSubC = newC.resolveFile("newsubc");
		newSubC.createFolder();

		FileObject newSubCF = newSubC.resolveFile("afile");
		OutputStream outputStream = newSubCF.getContent().getOutputStream();
		try {
			outputStream.write("This is the contents of a test file.".getBytes());
		} finally {
			outputStream.close();
		}
//		dumpFile(newC, 0);

		InputStream in = newSubCF.getContent().getInputStream();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			while ((line = br.readLine()) != null) {
				System.out.println(">>> " + line);
			}
		} finally {
			in.close();
		}

		newSubC.createFolder();

		if (newC.delete(new AllFileSelector()) == 0) {
			throw new IOException("Failed to delete");
		}
	}

	static void dumpFile(FileObject ob, int indent) throws FileSystemException {
		long sz = 0;
		FileType type = ob.getType();
		if (!type.equals(FileType.FOLDER))
			sz = ob.getContent().getSize();
		;
		System.out.println(String.format("%" + ((indent + 1) * 2) + "s %-30s %-10s %10d %8d", "",
				ob.getName().getBaseName(), type, sz, ob.getContent().getLastModifiedTime()));
		if (type.equals(FileType.FOLDER)) {
			for (FileObject c : ob.getChildren()) {
				dumpFile(c, indent + 1);
			}
		}
	}
}
