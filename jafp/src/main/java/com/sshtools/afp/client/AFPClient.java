package com.sshtools.afp.client;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.SocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.afp.client.AFPException.Error;
import com.sshtools.afp.client.AFPFile.Type;
import com.sshtools.afp.common.AFPBasicVolumeInfo;
import com.sshtools.afp.common.AFPConstants;
import com.sshtools.afp.common.AFPServerInfo;
import com.sshtools.afp.common.ObjectPool;
import com.sshtools.afp.server.AFPServerParams;
import com.sshtools.afp.server.DSI_Constants;
import com.sshtools.afp.server.DSI_Packet;

public class AFPClient {
	private static final Logger LOG = LoggerFactory.getLogger(AFPSession.class);
	private List<String> authenticationMethods = Arrays.asList(AFPConstants.UAM_STR_GUEST, AFPConstants.UAM_STR_DHX_128,
			AFPConstants.UAM_STR_CLEARTEXT);
	private String host;
	private int port;
	private SocketFactory socketFactory;
	private AFPAuthenticator authenticator;
	private AFPConnectionPool pool;
	private AFPServerInfo serverInfo;
	private ThreadLocal<Boolean> requireAuth = new ThreadLocal<>();
	private Object infoLock = new Object();
	{
		pool = new AFPConnectionPool();
	}

	public AFPClient() {
	}

	public AFPClient(String host) {
		this(host, 548);
	}

	public AFPClient(String host, int port) {
		this(host, port, null, null);
	}

	public AFPClient(String host, String username, char[] password) {
		this(host, 548, username, password);
	}

	public AFPClient(String host, int port, String username, char[] password) {
		this(host, port, username == null ? null : new BasicAuthenticator(username, password));
	}

	public AFPClient(String host, AFPAuthenticator authenticator) {
		this(host, 548, authenticator);
	}

	public AFPClient(String host, int port, AFPAuthenticator authenticator) {
		this.host = host;
		this.port = port;
		this.authenticator = authenticator;
	}

	public SocketFactory getSocketFactory() {
		return socketFactory;
	}

	public void setSocketFactory(SocketFactory socketFactory) {
		this.socketFactory = socketFactory;
	}

	public AFPAuthenticator getAuthenticator() {
		return authenticator;
	}

	public void setAuthenticator(AFPAuthenticator authenticator) {
		this.authenticator = authenticator;
	}

	public void setAuthenticationMethods(String... authenticationMethods) {
		setAuthenticationMethods(Arrays.asList(authenticationMethods));
	}

	public void setAuthenticationMethods(List<String> authenticationMethods) {
		this.authenticationMethods = authenticationMethods;
	}

	public List<String> getAuthenticationMethods() {
		return authenticationMethods;
	}

	public void close() throws IOException {
		pool.shutdown();
	}

	public AFPVolume get(String name) throws FileNotFoundException, IOException {
		for (AFPVolume vol : list()) {
			if (name.equals(vol.getName()))
				return vol;
		}
		throw new FileNotFoundException(String.format("No Volume named %s.", name));
	}

	public List<AFPVolume> list() throws IOException {
		List<AFPVolume> volumes = new ArrayList<>();
		AFPSession connection = pool.checkOut();
		try {
			AFPServerParams srv = new AFPServerParams(connection.sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST,
					DSI_Constants.CMD_COMMAND, connection.nextId(), new byte[] { AFPConstants.CMD_GET_SRVR_PARMS })).getReader());
			for (AFPBasicVolumeInfo v : srv.getVols()) {
				volumes.add(new AFPVolume(v, this));
			}
		} finally {
			pool.checkIn(connection);
		}
		return volumes;
	}

	public AFPServerInfo getServerInfo() throws IOException {
		synchronized (infoLock) {
			if (serverInfo == null) {
				/*
				 * Server will immediately close socket upon status, and we only
				 * we really currently need to do this once
				 */
				try (AFPStatus stat = new AFPStatus(createSocket())) {
					serverInfo = stat.getStatus();
				}
			}
		}
		return serverInfo;
	}

	void checkInAndExpire(AFPSession session) throws IOException {
		pool.checkInAndExpire(session);
		
	}
	
	void checkIn(AFPSession session) {
		pool.checkIn(session);
	}
	
	AFPSession checkOut(boolean requireAuth) throws IOException {
		Boolean was = this.requireAuth.get();
		this.requireAuth.set(requireAuth);
		try {
			return pool.checkOut();
		}
		finally {
			this.requireAuth.set(was);
		}
	}

	protected AFPSession createConnection() throws IOException {
		getServerInfo();
		List<String> authenticationMethods = new ArrayList<>(getAuthenticationMethods());
		if (authenticationMethods.isEmpty())
			throw new IllegalStateException("Client has been configured to support no authentication types.");
		if(Boolean.TRUE.equals(requireAuth.get())) {
			authenticationMethods.removeAll(Arrays.asList(AFPConstants.UAM_STR_GUEST));
		}
		LOG.info(String.format("Auth methods: %s.", authenticationMethods));
		
		while (!authenticationMethods.isEmpty()) {
			try {
				String am = authenticationMethods.get(0);
				AFPSession conx = new AFPSession(this, createSocket(), am, Arrays.asList(serverInfo.getUamModules()));
				LOG.info(String.format("Connection %s.", am));
				return conx;
			} catch (AFPException e) {
				if (Arrays.asList(Error.AUTHENTICATION_FAILED, Error.REQUIRE_AUTHENTICATION,
						Error.PASSWORD_TOO_LONG_FOR_AUTHENTICATION_TYPE).contains(e.getError()))
					authenticationMethods.remove(0);
				else
					throw e;
			}
		}
		throw new AFPException(Error.AUTHENTICATION_FAILED);
	}

	Socket createSocket() throws UnknownHostException, IOException {
		Socket s;
		if (socketFactory == null)
			s = new Socket(host, port);
		else
			s = socketFactory.createSocket(host, port);
		return s;
	}

	static void dump(AFPResource<?> z, int indent) throws IOException {
		StringBuilder bb = new StringBuilder();
		;
		for (int i = 0; i < indent; i++)
			bb.append(' ');
		System.out.println(bb.toString() + z);
		if (z instanceof AFPVolume || ((AFPFile) z).getType() == Type.FOLDER) {
			for (AFPResource<?> f : z.listFiles()) {
				dump(f, indent + 2);
			}
		}
	}

	class AFPConnectionPool extends ObjectPool<AFPSession, IOException> {
		@Override
		protected AFPSession create() throws IOException {
			return createConnection();
		}

		@Override
		public void expire(AFPSession o) throws IOException {
			o.close();
		}

		@Override
		public boolean validate(AFPSession o) {
			return o.isConnected();
		}
	}
}
