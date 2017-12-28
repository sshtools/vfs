package com.sshtools.vfs.s3.provider.s3;

import java.io.IOException;
import java.io.InputStream;

import com.amazonaws.services.s3.model.S3Object;

/**
 * Close a stream when it reaches EOF.
 * @author lee
 *
 */
public class S3InputStream extends InputStream {

	InputStream in;
	S3Object obj;
	boolean closed = false;
	
	public S3InputStream() {
		closed = true;
	}
	
	public S3InputStream(S3Object obj) {
		this.obj = obj;
		this.in = obj.getObjectContent();
	}

	@Override
	public int read() throws IOException {
		if(closed) {
			return -1;
		}
		int r = in.read();
		checkClose(r);
		return r;
	}
	
	@Override
	public int read(byte[] buf, int off, int len) throws IOException {
		if(closed) {
			return -1;
		}
		int r = in.read(buf, off, len);
		checkClose(r);
		return r;
	}
	
	private void checkClose(int r) throws IOException {
		if(r==-1) {
			in.close();
			obj.close();
			closed = true;
		}
	}

}
