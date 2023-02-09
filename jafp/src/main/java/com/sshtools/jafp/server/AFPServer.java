/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */
package com.sshtools.jafp.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.jafp.client.AFPSession;
import com.sshtools.jafp.common.AFPConstants;
import com.sshtools.jafp.common.AFPServerInfo;

/* TODO: request/reply queues
 *   * command 0x7a - client going to sleep?
 *
 * AFP BUGS:
 *   * sending truncated list for GetVolParams bitmap results in kernel panic / crash
 *   * Login (0x12) is *not* sending pad byte after command
 *   * failure to reply to DSI_TICKLE results in kernel panic
 *   * AFP31 p. 32 - offspring count is 2 bytes, NOT 4 bytes
 *   * sending TICKLE with 'reply' flag panics kernel
 *   * failing to complete an enumerate_ext2 call will panic/hang the system
 *   * responding to DSI_WRITE with mirror reply panics kernel
 */
public abstract class AFPServer implements AFPConstants, Runnable {
	private static final Logger LOG = LoggerFactory.getLogger(AFPSession.class);
	static boolean DEBUG_DEBUG = true;
	static boolean DEBUG_DSI = true;
	static boolean DEBUG_DSI_REQUEST = true && DEBUG_DSI;
	static boolean DEBUG_DSI_REPLY = true && DEBUG_DSI;
	static boolean DEBUG_DSI_LINE = true && DEBUG_DSI;
	public static boolean DEBUG_PRINT = DEBUG_DEBUG | DEBUG_DSI;
	private final static String[] protoStrings = { "AFP3.1", "AFP2.3" };
	private int port;
	private String bind;
	private ServerSocket socket;
	private String serverName;
	private Thread thread;
	private int nextVolID = 1;
	private Hashtable volumesByID;
	private Hashtable volumesByName;
	private JmDNS rendezvous;
	private AFPServerInfo serverInfo;
	private ServiceInfo serviceInfo;

	public AFPServer() throws IOException {
		this(TCP_PORT);
	}

	public AFPServer(int port) throws IOException {
		this(null, port);
	}

	public AFPServer(String hostname, int port) throws IOException {
		this(hostname, null, port);
	}

	public AFPServer(String hostname, String bind, int port) throws IOException {
		this(null, hostname, bind, port);
	}
	
	public AFPServer(String text, String serverName, String bind, int port) throws IOException {
		this.bind = bind;
		this.port = port;
		;
		this.volumesByID = new Hashtable();
		this.volumesByName = new Hashtable();
		// set debug level
		String dl = System.getProperty("debug.afp");
		if (dl != null && dl.length() > 0) {
			setDebugLevel(Integer.parseInt(dl));
		}
		// register server with Rendezvous
		InetAddress addr = null ;
		try {
			if(bind != null && !bind.equals("0.0.0.0"))
				addr = InetAddress.getByName(bind);
		} catch (UnknownHostException uhe) {
		}
		if(addr == null)
			addr = InetAddress.getLocalHost();
		this.serverName = serverName != null ? serverName : addr.getHostName();
		if (this.serverName != null && this.serverName.indexOf('.') > 0) {
			this.serverName = this.serverName.substring(0, this.serverName.indexOf('.'));
		}
		
		if(text == null) {
			text = this.serverName + " AFP Service";
		}

		registerMDNS(text, this.serverName, port);
	}

	protected void registerMDNS(String text, String serverName, int port) throws IOException {
		this.rendezvous = JmDNS.create();
		serviceInfo = ServiceInfo.create("_afpovertcp._tcp.local.", serverName, port, text);
		rendezvous.registerService(serviceInfo);
	}
	protected void unregisterMDNS() throws IOException {
		if(serviceInfo != null)
			rendezvous.unregisterService(serviceInfo); 
	}

	public void setDebugLevel(int lvl) {
		DEBUG_DEBUG = true;
		DEBUG_DSI = true;
		DEBUG_DSI_REQUEST = DEBUG_DSI;
		DEBUG_DSI_REPLY = DEBUG_DSI;
		DEBUG_DSI_LINE = DEBUG_DSI;
		DEBUG_PRINT = DEBUG_DEBUG | DEBUG_DSI;
		switch (lvl) {
		case 0:
			DEBUG_DEBUG = false;
			DEBUG_PRINT = false;
			DEBUG_DSI = false;
			break;
		case 1:
			DEBUG_DSI_REQUEST = false;
			DEBUG_DSI_REPLY = false;
			break;
		default:
			break;
		}
		DEBUG_DSI_REQUEST &= DEBUG_DSI;
		DEBUG_DSI_REPLY &= DEBUG_DSI;
		DEBUG_DSI_LINE &= DEBUG_DSI;
	}

	public synchronized int addVolume(AFPServerVolume vol) {
		int id = nextVolID++;
		volumesByID.put(Integer.valueOf(id), vol);
		volumesByName.put(vol.getName(), vol);
		vol.setID(id);
		return id;
	}

	public AFPServerVolume getVolume(int vid) {
		return (AFPServerVolume) volumesByID.get(Integer.valueOf(vid));
	}

	public AFPServerVolume getVolume(String vname) {
		return (AFPServerVolume) volumesByName.get(vname);
	}

	public synchronized AFPServerVolume[] getVolumes() {
		Object k[] = volumesByName.keySet().toArray();
		AFPServerVolume v[] = new AFPServerVolume[k.length];
		for (int i = 0; i < k.length; i++) {
			v[i] = (AFPServerVolume) volumesByName.get(k[i]);
		}
		return v;
	}

	public synchronized void delVolume(AFPServerVolume vol) {
		if (vol == null) {
			return;
		}
		volumesByName.remove(vol.getName());
		volumesByID.remove(Integer.valueOf(vol.getID()));
	}

	public void delVolume(String vname) {
		delVolume((AFPServerVolume) volumesByName.get(vname));
	}

	public void delVolume(int vid) {
		delVolume((AFPServerVolume) volumesByID.get(Integer.valueOf(vid)));
	}

	public synchronized void start() throws IOException {
		if (thread != null) {
			return;
		}
		socket = bind != null ? new ServerSocket(port, 10, InetAddress.getByName(bind)) : new ServerSocket(port);
		thread = new Thread(this, "AFP Server");
		thread.start();
	}

	public void stop() {
		if(socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
			}
			try {
				unregisterMDNS();
			} catch (IOException e) {
			}
		}
		
	}

	public void run() {
		try {
			LOG.info("Jaffer AFP/TCP Server v" + Main.VERSION + " ready on port " + socket.getLocalPort() + " as '"
					+ serverName + "'");
			while (true) {
				acceptConnection();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public abstract boolean hasCleartextPasswords();

	public abstract boolean hasUser(String userName);

	public abstract boolean checkPassword(String userName, String password);

	public abstract boolean setThreadOwner(String userName);

	public abstract String getPassword(String userName);

	public abstract String getGuestUser();

	public AFPServerInfo getServerInfo() {
		if (serverInfo == null) {
			serverInfo = new AFPServerInfo(serverName, protoStrings,
					hasCleartextPasswords()
							? new String[] { UAM_STR_GUEST, UAM_STR_CLEARTEXT, UAM_STR_RANDOM_NUM1, UAM_STR_DHX_128 }
							: new String[] { UAM_STR_GUEST, UAM_STR_CLEARTEXT, UAM_STR_DHX_128 },
					0);
		}
		return serverInfo;
	}

	protected void acceptConnection() throws IOException {
		Socket s = socket.accept();
		startFromSocket(s);
	}

	protected void startFromSocket(Socket socket) throws IOException {
		socket.setTcpNoDelay(true);
		LOG.info("AFP_Server: connect from " + socket.getInetAddress());
		AFPServerSession session = new AFPServerSession(this, socket);
		session.start();
	}
}
