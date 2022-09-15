package com.sshtools.jafp.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.jafp.client.AFPException.Error;
import com.sshtools.jafp.server.DSI_Packet;

public class AbstractAFPConnection implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(AFPSession.class);
	protected InputStream in;
	protected OutputStream out;
	private int reqID = 0;
	private Socket socket;

	protected AbstractAFPConnection(Socket socket) throws IOException {
		this.socket = socket;
		out = socket.getOutputStream();
		in = socket.getInputStream();
	}

	@Override
	public void close() throws IOException {
		LOG.debug("Close");
		if(socket == null)
			throw new IOException("Not open.");
		socket.close();
	}

	public Socket getSocket() {
		return socket;
	}

	public boolean isConnected() {
		return socket != null && !socket.isClosed();
	}

	public synchronized int nextId() {
		if (reqID == 65535)
			reqID = 0;
		else
			reqID++;
		return reqID;
	}

	public InputStream getIn() {
		return in;
	}

	public OutputStream getOut() {
		return out;
	}

	public DSI_Packet sendRecv(DSI_Packet dp) throws IOException {
		synchronized (out) {
			dp.write(out);
			LOG.debug("# -----------------------------------------------");
			LOG.debug("send=(" + dp + ")");
			dp.dumpSendPayload("> ");
			dp.read(in);
			LOG.debug("recv=(" + dp + ")");
			LOG.debug("< ");
			if (dp.isReply() && dp.getErrorCode() != 0) {
				throw new AFPException(Error.SERVER_ERROR, String.format("Server returned error " + dp.getErrorCode()))
						.setCode(dp.getErrorCode());
			}
			return dp;
		}
	}

	public int sendRecvCode(DSI_Packet dp) throws IOException {
		synchronized (out) {
			dp.write(out);
			LOG.debug("# -----------------------------------------------");
			LOG.debug("send=(" + dp + ")");
			dp.dumpSendPayload("> ");
			dp.read(in);
			LOG.debug("recv=(" + dp + ")");
			LOG.debug("< ");
			if (dp.isReply() && dp.getErrorCode() != 0) {
				return dp.getErrorCode();
			}
			return 0;
		}
	}
}
