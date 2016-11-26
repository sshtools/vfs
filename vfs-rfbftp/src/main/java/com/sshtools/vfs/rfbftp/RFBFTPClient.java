package com.sshtools.vfs.rfbftp;

import java.io.IOException;

import com.sshtools.rfb.DummyDisplay;
import com.sshtools.rfb.RFBAuthenticationException;
import com.sshtools.rfb.RFBContext;
import com.sshtools.rfb.RFBDisplay;
import com.sshtools.rfb.RFBEventHandler;
import com.sshtools.rfb.RFBSocketTransport;

public class RFBFTPClient {
	private RFBSocketTransport transport;
	private RFBDisplay<?, ?> display;
	private boolean shared;

	public RFBFTPClient(RFBDisplay<?, ?> display) {
		this.display = display;
		shared = true;
	}

	public boolean isShared() {
		return shared;
	}

	public RFBFTPClient(RFBContext context, String hostname, int port, RFBEventHandler events)
			throws IOException, RFBAuthenticationException {
		transport = new RFBSocketTransport(hostname, port);
		display = new DummyDisplay(context);
		display.initialiseSession(transport, context, events);
		display.getEngine().startRFBProtocol();
	}

	public RFBDisplay<?, ?> getDisplay() {
		return display;
	}
}
