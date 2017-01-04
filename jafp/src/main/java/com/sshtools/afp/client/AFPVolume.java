package com.sshtools.afp.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.afp.common.AFPBasicVolumeInfo;
import com.sshtools.afp.common.AFPConstants;
import com.sshtools.afp.common.AFPVolumeInfo;
import com.sshtools.afp.common.ByteWriter;
import com.sshtools.afp.server.AFPNodeInfo;
import com.sshtools.afp.server.DSI_Constants;
import com.sshtools.afp.server.DSI_Packet;

public class AFPVolume implements AFPResource<AFPFile> {
	private String name;
	private char[] password;
	private AFPClient client;
	private AFPVolumeInfo volumeInfo;
	private boolean passwordProtected;
	private boolean hasUnixPrivs;
	private boolean authRequired;

	public AFPVolume(String name, AFPClient client) {
		this(name, client, false, false);
	}

	public AFPVolume(String name, AFPClient client, boolean hasUnixPrivs, boolean passwordProtected) {
		this.client = client;
		this.name = name;
		this.hasUnixPrivs = hasUnixPrivs;
		this.passwordProtected = passwordProtected;
	}

	AFPVolume(AFPBasicVolumeInfo info, AFPClient client) {
		this.client = client;
		this.name = info.getName();
	}

	public char[] getPassword() {
		return password;
	}

	public void setPassword(char[] password) {
		this.password = password;
	}

	public void open() throws IOException {
		authRequired = false;
		for (int i = 0 ; i < 2; i++) {
			AFPSession connection = client.checkOut(authRequired);
			boolean expire = false;
			try {
				ByteWriter ww = new ByteWriter(128);
				ww.writeByte(AFPConstants.CMD_OPEN_VOL);
				ww.writeByte(0);
				ww.writeShort(0xffff); // flags
				ww.writePString(name);
				if (name.length() % 2 == 0) {
					ww.writeByte(0);
				}
				if (password != null)
					ww.writeBytes(new String(password));
				try {
					volumeInfo = new AFPVolumeInfo(connection.sendRecv(new DSI_Packet(DSI_Constants.DSI_REQUEST,
							DSI_Constants.CMD_COMMAND, connection.nextId(), ww.toByteArray())).getReader());
					break;
				} catch (AFPException e) {
					if (e.getCode() == AFPConstants.ERR_BITMAP_ERR && !authRequired) {
						// TODO Seems a bit odd, check this is a good error to
						// check for when not authenticated
						authRequired = true;
						expire = true;
					} else
						throw e;
				}
			} finally {
				if (expire)
					client.checkInAndExpire(connection);
				else
					client.checkIn(connection);
			}
		}
	}

	public boolean isHasUnixPrivs() {
		return hasUnixPrivs;
	}

	public boolean isPasswordProtected() {
		return passwordProtected;
	}

	public String getName() {
		return name;
	}

	public AFPClient getClient() {
		return client;
	}

	public AFPFile get(String path) throws IOException {
		return new AFPFile(path, this);
	}

	@Override
	public String toString() {
		return "AFPVolume [client=" + client + ", passwordProtected=" + passwordProtected + ", hasUnixPrivs=" + hasUnixPrivs
				+ ", name=" + name + "]";
	}

	protected void checkOpen() throws IOException {
		if (volumeInfo == null)
			open();
	}

	@Override
	public List<AFPFile> listFiles() throws IOException {
		checkOpen();
		AFPSession connection = client.checkOut(false);
		List<AFPFile> names = new ArrayList<>();
		try {
			// TODO add more .... something goes wrong when ALL are added
			int fileFlags = AFPConstants.FILE_BIT_LONG_NAME | AFPConstants.FILE_BIT_DATA_FORK_LEN | AFPConstants.FILE_BIT_MOD_DATE
					| AFPConstants.FILE_BIT_NODE_ID;
			int dirFlags = AFPConstants.DIR_BIT_LONG_NAME | AFPConstants.DIR_BIT_NODE_ID;
			for (AFPNodeInfo info : connection.enumerate(volumeInfo.getId(), ROOT_NODE_ID, fileFlags, dirFlags,
					AFPConstants.MODE_OLD, "")) {
				names.add(new AFPFile(info, this));
			}
		} finally {
			client.checkIn(connection);
		}
		return names;
	}

	@Override
	public List<String> list() throws IOException {
		checkOpen();
		AFPSession connection = client.checkOut(false);
		List<String> names = new ArrayList<>();
		try {
			int fileFlags = AFPConstants.FILE_BIT_SHORT_NAME;
			int dirFlags = AFPConstants.DIR_BIT_SHORT_NAME;
			for (AFPNodeInfo info : connection.enumerate(volumeInfo.getId(), ROOT_NODE_ID, fileFlags, dirFlags,
					AFPConstants.MODE_OLD, "")) {
				names.add(info.getShortName());
			}
		} finally {
			client.checkIn(connection);
		}
		return names;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AFPVolume other = (AFPVolume) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public int getId() throws IOException {
		checkOpen();
		return volumeInfo.getId();
	}
}
