package com.sshtools.vfs.smbng;

import jcifs.CIFSContext;
import jcifs.CloseableIterator;
import jcifs.SmbResource;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;

public class Test1 {
	public static void main(String[] args) throws Exception {
		CIFSContext ctx = SingletonContext.getInstance();
//		ctx = ctx.withCredentials(new NtlmPasswordAuthenticator("router", "flipper"));
//		SmbResource res = ctx.get("smb://fat/public");
		ctx = ctx.withCredentials(new NtlmPasswordAuthenticator("hypersocket.local", "bsmith", "Yarim123?"));
		SmbResource res = ctx.get("smb://dc/Public");
		try (CloseableIterator<SmbResource> r = res.children()) {
			while (r.hasNext()) {
				System.out.println(r.next().toString());
			}
		}
	}
}
