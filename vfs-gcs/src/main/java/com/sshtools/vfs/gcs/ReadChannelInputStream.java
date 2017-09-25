package com.sshtools.vfs.gcs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.google.cloud.ReadChannel;

public class ReadChannelInputStream extends InputStream {

	ReadChannel channel;
	ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
	
	public ReadChannelInputStream(ReadChannel channel) {
		this.channel = channel;
	}
	
	@Override
	public int read() throws IOException {
		if(channel==null) {
			return -1;
		}
		byte[] b = new byte[1];
		int r = read(b);
		if(r > 0) {
			return b[0] & 0xFF;
		}
		return r;
	}

	public synchronized int read(byte[] buf, int off, int len) throws IOException {
		if(channel==null) {
			return -1;
		}
		int limit = bytes.limit();
		if(len < bytes.remaining()) {
			bytes.limit(len);
		}
		int res = channel.read(bytes);
		if(res < 0) {
			close();
			return -1;
		}
		bytes.flip();
		int read = Math.min(bytes.remaining(), len);
		bytes.get(buf, off, read);
		bytes.compact();
		bytes.limit(limit);
		return read;
	}
	
	public synchronized void close() {
		if(channel!=null) {
			channel.close();
			channel = null;
		}
	}
}
