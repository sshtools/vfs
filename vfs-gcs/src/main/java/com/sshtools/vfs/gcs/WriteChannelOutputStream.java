package com.sshtools.vfs.gcs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;

import com.google.cloud.WriteChannel;

public class WriteChannelOutputStream extends OutputStream {

	WriteChannel channel;
	ByteBuffer bytes = ByteBuffer.allocate(64 * 1024);
	
	public WriteChannelOutputStream(WriteChannel channel) {
		this.channel = channel;
	}
	
	@Override
	public void write(int b) throws IOException {
		if(channel==null) {
			throw new ClosedChannelException();
		}
		bytes.put((byte)b);
		bytes.flip();
		channel.write(bytes);
		bytes.compact();
	}

	public synchronized void write(byte[] buf, int off, int len) throws IOException {
		if(channel==null) {
			throw new ClosedChannelException();
		}
		int count = 0;
		while(count < len) {
			int c = Math.min(len-count, bytes.remaining());
			bytes.put(buf, off+count, c);
			bytes.flip();
			while(bytes.hasRemaining()) {
				channel.write(bytes);
			}
			bytes.compact();
			count += c;
		}
	}
	
	public synchronized void close() throws IOException {
		if(channel!=null) {
			channel.close();
			channel = null;
		}
	}
}
