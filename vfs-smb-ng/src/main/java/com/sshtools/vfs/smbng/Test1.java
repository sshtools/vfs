package com.sshtools.vfs.smbng;

import jcifs.CIFSContext;
import jcifs.CloseableIterator;
import jcifs.SmbResource;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;

public class Test1 {
	public static void main(String[] args) throws Exception {
		CIFSContext ctx = SingletonContext.getInstance();
//		ctx = ctx.withCredentials(new NtlmPasswordAuthenticator("router", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXX"));
//		SmbResource res = ctx.get("smb://fat/public");
		ctx = ctx.withCredentials(new NtlmPasswordAuthenticator("hypersocket", "bsmith", "XXXXXXXXXXXXXXXXXXXXXXXXXx"));
		SmbResource res = ctx.get("smb://nas.hypersocket.io/Public");
		try (CloseableIterator<SmbResource> r = res.children()) {
			while (r.hasNext()) {
				System.out.println(r.next().toString());
			}
		}
	}
}
