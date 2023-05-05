package com.sshtools.jafp.client;

import java.io.IOException;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.jafp.common.AFPServerInfo;
import com.sshtools.jafp.server.DSI_Constants;
import com.sshtools.jafp.server.DSI_Packet;

public class AFPStatus extends AbstractAFPConnection {
	private static final Logger LOG = LoggerFactory.getLogger(AFPSession.class);

	private AFPServerInfo serverInfo;

	private int rx_quantum;

	public AFPStatus(Socket socket) throws IOException {
		super(socket);
		
		/*
		 * Get the status, and calculate the transmit time. We use this to
		 * calculate our rx quantum.
		 */
		long then = System.currentTimeMillis();
		serverInfo = new AFPServerInfo(
				sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST, DSI_Constants.CMD_GET_STATUS, nextId(), new byte[0]))
						.getReader());
		long now = System.currentTimeMillis();
		long delay = now - then;
		/* Calculate the quantum based on our tx_delay and a threshold */
		/* For now, we'll just set a default */
		/* This is the default in 10.4.x where x > 7 */
		rx_quantum = 128 * 1024;
		LOG.debug("srvinfo=(" + serverInfo + ")");
		LOG.info(String.format("Server %s:%s ", socket.getInetAddress().getHostName(), socket.getPort()));
		LOG.info(String.format("Initial delay of %d, calculated quant as %d", delay, rx_quantum));
		LOG.info(String.format("Server name: %s", serverInfo.getServerName()));
		LOG.info("Supports versions :-");
		for (String s : serverInfo.getAfpVersions()) {
			LOG.info("  " + s);
		}
		LOG.info("Server supports authentication:-");
		for (String s : serverInfo.getUamModules()) {
			LOG.info("  " + s);
		}
	}
	
	public AFPServerInfo getStatus() {
		return serverInfo;
	}
}
