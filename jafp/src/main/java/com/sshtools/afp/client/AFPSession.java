package com.sshtools.afp.client;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sshtools.afp.client.AFPAuthenticator.AuthDetail;
import com.sshtools.afp.client.AFPException.Error;
import com.sshtools.afp.common.AFPConstants;
import com.sshtools.afp.common.AFPDirectoryInfo;
import com.sshtools.afp.common.AFPFileInfo;
import com.sshtools.afp.common.AFPVolumeInfo;
import com.sshtools.afp.common.ByteReader;
import com.sshtools.afp.common.ByteWriter;
import com.sshtools.afp.common.Utility;
import com.sshtools.afp.server.AFPNodeInfo;
import com.sshtools.afp.server.CAST128;
import com.sshtools.afp.server.DSI_Constants;
import com.sshtools.afp.server.DSI_Packet;

public class AFPSession extends AbstractAFPConnection {
	private static final Logger LOG = LoggerFactory.getLogger(AFPSession.class);
	private final static Random random = new Random();;
	private short sessionId;
	private int rx_quantum;

	AFPSession(AFPClient afpClient, Socket socket, String auth, List<String> serverModules) throws IOException {
		super(socket);
		LOG.info(String.format("Will attempt to use %s.", auth));
		// TODO why and when?
		boolean useLoginExt = true;
		// Is clear text supported, do we have authenticator?
		try {
			if (serverModules.contains(AFPConstants.UAM_STR_DHX_128) && auth.equals(AFPConstants.UAM_STR_DHX_128)
					&& afpClient.getAuthenticator() != null) {
				loginDhx128(afpClient, auth, useLoginExt);
			} else if (serverModules.contains(AFPConstants.UAM_STR_CLEARTEXT) && auth.equals(AFPConstants.UAM_STR_CLEARTEXT)
					&& afpClient.getAuthenticator() != null) {
				loginClearText(afpClient, auth);
			} else if (serverModules.contains(AFPConstants.UAM_STR_GUEST) && auth.equals(AFPConstants.UAM_STR_GUEST)) {
				loginGuest();
			} else
				throw new AFPException(Error.REQUIRE_AUTHENTICATION,
						String.format(
								"Client authentication %s configuration not applicable for valid server authentication methods (%s). ",
								auth, serverModules));
		} catch (IOException ioe) {
			try {
				close();
			} catch (Exception e) {
			}
			throw ioe;
		}
	}

	@Override
	public void close() throws IOException {
		LOG.debug("Close");
		synchronized (out) {
			try {
				sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST, DSI_Constants.CMD_CLOSE_SESSION, nextId(), new byte[0]));
			} catch (EOFException e) {
			}
			super.close();
		}
	}

	public short getSessionId() {
		return sessionId;
	}

	List<AFPNodeInfo> enumerate(int volId, int dirId, int fileFlags, int dirFlags, int mode, String path) throws IOException {
		List<AFPNodeInfo> names = new ArrayList<>();
		// TODO must sort out buffer sizes... this should not be required!
		ByteWriter ww = new ByteWriter(1024);
		ww.writeByte(AFPConstants.CMD_ENUMERATE);
		ww.writeByte(0);
		ww.writeShort(volId);
		ww.writeInt(dirId); // I think?
		ww.writeShort(fileFlags);
		ww.writeShort(dirFlags);
		ww.writeShort(Short.MAX_VALUE); // TODO paging?
		ww.writeShort(1); // Started index+1?
		ww.writeShort(Short.MAX_VALUE); // TODO paging?
		ww.write(0);
		ww.writePString(path);
		ByteReader r = sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST, DSI_Constants.CMD_COMMAND, nextId(), ww.toByteArray()))
				.getReader();
		fileFlags = r.readUnsignedShort();
		dirFlags = r.readUnsignedShort();
		int records = r.readUnsignedShort();
		for (int i = 0; i < records; i++) {
			int len = r.readUnsignedByte();
			byte[] rz;
			if (len % 2 != 0) {
				rz = new byte[len - 2];
				r.readBytes(rz);
				r.readUnsignedByte();
			} else {
				rz = new byte[len - 1];
				r.readBytes(rz);
			}
			ByteReader brz = new ByteReader(rz);
			int type = mode == AFPConstants.MODE_OLD ? brz.readUnsignedByte() : brz.readUnsignedShort();
			if (type == 0) {
				names.add(new AFPFileInfo(fileFlags, brz));
			} else {
				names.add(new AFPDirectoryInfo(dirFlags, brz));
			}
		}
		return names;
	}

	AFPVolumeInfo getVolume(int id, int flags) throws IOException {
		ByteWriter ww = new ByteWriter(32);
		ww.write(AFPConstants.CMD_GET_VOL_PARMS);
		ww.write(0);
		ww.writeShort(id);
		ww.writeShort(flags);
		return new AFPVolumeInfo(
				sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST, DSI_Constants.CMD_COMMAND, nextId(), ww.toByteArray()))
						.getReader());
	}

	//
	AFPNodeInfo info(int volId, int dirId, int fileFlags, int dirFlags, int mode, String path) throws IOException {
		ByteWriter ww = new ByteWriter(1024);
		ww.writeByte(AFPConstants.CMD_GET_FILE_DIR_PARMS);
		ww.writeByte(0);
		ww.writeShort(volId);
		ww.writeInt(dirId); // I think?
		ww.writeShort(fileFlags);
		ww.writeShort(dirFlags);
		ww.write(0);
		ww.writePString(path);
		ByteReader r = sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST, DSI_Constants.CMD_COMMAND, nextId(), ww.toByteArray()))
				.getReader();
		fileFlags = r.readUnsignedShort();
		dirFlags = r.readUnsignedShort();
		int type = mode == AFPConstants.MODE_OLD ? r.readUnsignedByte() : r.readUnsignedShort();
		if (type == 0) {
			return new AFPFileInfo(fileFlags, r);
		} else {
			return new AFPDirectoryInfo(dirFlags, r);
		}
	}

	protected void openSession() throws IOException {
		ByteWriter ww = new ByteWriter(256);
		ww.write(DSI_Constants.OPT_ATTN_QUANT);
		ww.write(4);
		ww.writeInt(rx_quantum);
		sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST, DSI_Constants.CMD_OPEN_SESSION, nextId(), ww.toByteArray()));
	}

	private void loginClearText(AFPClient afpClient, String auth) throws AFPException, IOException {
		Map<AuthDetail, char[]> a = afpClient.getAuthenticator().authenticate(AuthDetail.USERNAME, AuthDetail.PASSWORD);
		if (a == null)
			throw new AFPException(Error.AUTHENTICATION_CANCELLED);
		if (a.isEmpty())
			throw new AFPException(Error.REQUIRE_AUTHENTICATION,
					String.format("Client authentication %s requires a username and/or password none has been provider.", auth));
		String username = new String(a.get(AuthDetail.USERNAME));
		char[] password = a.get(AuthDetail.PASSWORD);
		if (password == null)
			password = "".toCharArray();
		if (password.length > 8)
			throw new AFPException(Error.PASSWORD_TOO_LONG_FOR_AUTHENTICATION_TYPE,
					String.format(
							"The password supplied is %d characters long, but only %d are supported by the %s authentication method.",
							password.length, 8, AFPConstants.UAM_STR_CLEARTEXT));
		openSession();
		ByteWriter ww = new ByteWriter(128);
		ww.writeByte(AFPConstants.CMD_LOGIN_EXT);
		ww.writeByte(0);
		ww.writeShort(1);
		ww.writePString(AFPConstants.AFP_PROTOCOL_VERSION);
		ww.writePString(AFPConstants.UAM_STR_CLEARTEXT);
		ww.writeTypedPString(username);
		ww.writeTypedPString(""); // TODO paName, what is this?
		if (ww.getOffset() % 2 == 1)
			ww.write(0);
		ww.writeCString(new String(password), 8);
		try {
			sessionId = sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST, DSI_Constants.CMD_COMMAND, nextId(), ww.toByteArray()))
					.getReader().readShort();
			LOG.info("Logged in OK as %s with session ID of %d", username, sessionId);
		} catch (AFPException a2) {
			throw new AFPException(Error.AUTHENTICATION_FAILED, a2);
		}
	}

	private void loginDhx128(AFPClient afpClient, String auth, boolean useLoginExt) throws AFPException, IOException {
		// Up to 16 characters password
		Map<AuthDetail, char[]> a = afpClient.getAuthenticator().authenticate(AuthDetail.USERNAME, AuthDetail.PASSWORD);
		if (a == null)
			throw new AFPException(Error.AUTHENTICATION_CANCELLED);
		if (a.isEmpty())
			throw new AFPException(Error.REQUIRE_AUTHENTICATION,
					String.format("Client authentication %s requires a username and/or password none has been provider.", auth));
		String username = new String(a.get(AuthDetail.USERNAME));
		char[] password = a.get(AuthDetail.PASSWORD);
		openSession();
		if (password == null)
			password = "".toCharArray();
		if (password.length > 15)
			throw new AFPException(Error.PASSWORD_TOO_LONG_FOR_AUTHENTICATION_TYPE,
					String.format(
							"The password supplied is %d characters long, but only %d are supported by the %s authentication method.",
							password.length, 15, AFPConstants.UAM_STR_DHX_128));
		ByteWriter ww = new ByteWriter(128);
		if (useLoginExt) {
			ww.writeByte(AFPConstants.CMD_LOGIN_EXT);
			ww.writeByte(0);
			ww.writeShort(1);
			ww.writePString(AFPConstants.AFP_PROTOCOL_VERSION);
			ww.writePString(AFPConstants.UAM_STR_DHX_128);
			ww.writeTypedPString(username);
			ww.writeTypedPString(""); // TODO paName, what is this?
		} else {
			ww.writeByte(AFPConstants.CMD_LOGIN);
			ww.writePString(AFPConstants.AFP_PROTOCOL_VERSION);
			ww.writePString(AFPConstants.UAM_STR_DHX_128);
			ww.writePString(username);
		}
		if (ww.getOffset() % 2 == 1)
			ww.write(0);
		BigInteger ra = new BigInteger(128, random).abs();
		BigInteger ma = AFPConstants.DHX_G.modPow(ra, AFPConstants.DHX_P); //
		byte[] maBytes = Utility.keyBytes(ma);
		ww.writeBytes(maBytes);
		DSI_Packet pkt = new DSI_Packet(DSI_Constants.DSI_REQUEST, DSI_Constants.CMD_COMMAND, nextId(), ww.toByteArray());
		try {
			sendRecv(pkt);
		} catch (AFPException e) {
			if (e.getCode() != AFPConstants.ERR_AUTH_CONTINUE) {
				throw e;
			}
		}
		ByteReader r = pkt.getReader();
		sessionId = r.readShort();
		byte[] serverKeyBytes = new byte[16];
		r.readBytes(serverKeyBytes);
		/**
		 * Contains the nonce (16 bytes) and the server signature (which we dont
		 * use)
		 */
		byte[] ciphertext = new byte[32];
		r.readBytes(ciphertext);
		ww = new ByteWriter(256);
		ww.writeByte(AFPConstants.CMD_LOGIN_CONT);
		ww.write(0);
		ww.writeShort(sessionId);
		BigInteger mb = new BigInteger(1, serverKeyBytes);
		BigInteger k = mb.modPow(ra, AFPConstants.DHX_P);
		/*
		 * FIXME: To support the Reconnect UAM, we need to stash this key
		 * somewhere in the session data. We'll worry about doing that later,
		 * but this would be a prime spot to do that.
		 */
		CAST128 c = new CAST128(Utility.keyBytes(k));
		/* Decrypt the ciphertext from the server. */
		byte[] decrypted = new byte[32];
		c.decrypt(ciphertext, 0, decrypted, 0, 32, AFPConstants.DHX_S2CIV);
		byte[] nonced = new byte[16];
		System.arraycopy(decrypted, 0, nonced, 0, 16);
		BigInteger nonce = new BigInteger(nonced);
		/* Increment the nonce by 1 for sending back to the server. */
		nonce = nonce.add(new BigInteger("1"));
		/*
		 * New plaintext is 16 bytes of nonce, and (up to) 64 bytes of password
		 * (filled out with NULL values).
		 */
		byte[] pstr = new String(password).getBytes();
		byte[] inbuf = new byte[16 + 64];
		System.arraycopy(nonced, 0, inbuf, 0, 16);
		System.arraycopy(pstr, 0, inbuf, 16, Math.min(inbuf.length - 1 - 16, pstr.length));
		byte[] outbuf = new byte[16 + 64];
		c.encrypt(inbuf, 0, outbuf, 0, pstr.length + 16, AFPConstants.DHX_C2SIV);
		ww.writeBytes(outbuf);
		try {
			sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST, DSI_Constants.CMD_COMMAND, nextId(), ww.toByteArray()));
			LOG.info(String.format("Logged in OK as %s with session ID of %d", username, sessionId));
		} catch (AFPException a2) {
			throw new AFPException(Error.AUTHENTICATION_FAILED, a2);
		}
	}

	private void loginGuest() throws IOException, AFPException {
		openSession();
		ByteWriter ww = new ByteWriter(128);
		ww.writeByte(AFPConstants.CMD_LOGIN);
		ww.writePString(AFPConstants.AFP_PROTOCOL_VERSION);
		ww.writePString(AFPConstants.UAM_STR_GUEST);
		try {
			sessionId = sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST, DSI_Constants.CMD_COMMAND, nextId(), ww.toByteArray()))
					.getReader().readShort();
			LOG.info(String.format("Logged in OK as guest with session ID of %d", sessionId));
		} catch (AFPException a) {
			throw new AFPException(Error.AUTHENTICATION_FAILED, a);
		}
	}
}