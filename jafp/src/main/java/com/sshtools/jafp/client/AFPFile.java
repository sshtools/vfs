package com.sshtools.jafp.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sshtools.jafp.common.AFPConstants;
import com.sshtools.jafp.common.AFPDirectoryInfo;
import com.sshtools.jafp.common.AFPFileInfo;
import com.sshtools.jafp.server.AFPNodeInfo;

public class AFPFile implements AFPResource<AFPFile> {
	public enum Type {
		FILE, FOLDER, IMAGINARY
	}

	public final static int DEFAULT_FILE_FLAGS = AFPConstants.FILE_BIT_LONG_NAME | AFPConstants.FILE_BIT_DATA_FORK_LEN
			| AFPConstants.FILE_BIT_MOD_DATE | AFPConstants.FILE_BIT_NODE_ID;
	public final static int DEFAULT_DIR_FLAGS = AFPConstants.DIR_BIT_LONG_NAME | AFPConstants.DIR_BIT_NODE_ID;
	private AFPVolume volume;
	private String path;
	private AFPFile parent;
	private AFPNodeInfo info;

	public AFPFile(AFPFile parent, String path) throws IOException {
		this.path = AFPUtil.removeTrailingSeparator(path);
		this.volume = parent.getVolume();
		if (!path.startsWith("/")) {
			this.parent = parent;
		}
		info = getInfo();
	}

	public AFPFile(String path, AFPVolume volume) throws IOException {
		this.volume = volume;
		this.path = AFPUtil.removeTrailingSeparator(path);
		var pp = getParentPath();
		this.parent = pp == null ? null : new AFPFile(pp, volume);
		info = getInfo();
	}

	AFPFile(AFPNodeInfo info, AFPVolume volume) {
		this.volume = volume;
		this.path = "/" + info.getLongName();
	}

	AFPFile(AFPNodeInfo info, AFPFile parent) {
		this.volume = parent.getVolume();
		this.parent = parent;
		this.info = info;
		this.path = AFPUtil.appendPaths(parent.getPath(), info.getLongName());
	}

	public Type getType() throws IOException {
		var info = getInfo();
		if (info instanceof AFPFileInfo)
			return Type.FILE;
		else if (info instanceof AFPDirectoryInfo)
			return Type.FOLDER;
		return Type.IMAGINARY;
	}
	
	public void refresh() {
		info = null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((volume == null) ? 0 : volume.hashCode());
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
		AFPFile other = (AFPFile) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (volume == null) {
			if (other.volume != null)
				return false;
		} else if (!volume.equals(other.volume))
			return false;
		return true;
	}

	public String getAbsolutePath() {
		return isAbsolute() || getParent() == null ? getPath() : AFPUtil.appendPaths(getParent().getAbsolutePath(), getName());
	}

	public String getParentPath() {
		if (path.equals("/")) {
			return null;
		}
		return null;
	}

	public AFPFile getParent() {
		return parent;
	}

	public String getPath() {
		return path;
	}

	public boolean isAbsolute() {
		return path.startsWith("/");
	}

	public AFPVolume getVolume() {
		return volume;
	}

	@Override
	public String getName() {
		int idx = path.lastIndexOf('/');
		return idx == -1 ? path : path.substring(idx + 1);
	}

	@Override
	public List<String> list() throws IOException {
		volume.checkOpen();
		var connection = volume.getClient().checkOut(false);
		var names = new ArrayList<String>();
		try {
			int fileFlags = AFPConstants.FILE_BIT_SHORT_NAME;
			int dirFlags = AFPConstants.DIR_BIT_SHORT_NAME;
			for (var info : connection.enumerate(volume.getId(), ROOT_NODE_ID, fileFlags, dirFlags, AFPConstants.MODE_OLD,
					AFPUtil.removeLeadingSlash(getAbsolutePath()))) {
				names.add(info.getShortName());
			}
		} finally {
			volume.getClient().checkIn(connection);
		}
		return names;
	}

	@Override
	public List<AFPFile> listFiles() throws IOException {
		volume.checkOpen();
		var connection = volume.getClient().checkOut(false);
		List<AFPFile> names = new ArrayList<>();
		try {
			// TODO add more .... something goes wrong when ALL are added
			for (var info : connection.enumerate(volume.getId(), ROOT_NODE_ID, DEFAULT_FILE_FLAGS, DEFAULT_DIR_FLAGS,
					AFPConstants.MODE_OLD, AFPUtil.removeLeadingSlash(getAbsolutePath()))) {
				names.add(new AFPFile(info, this));
			}
		} finally {
			volume.getClient().checkIn(connection);
		}
		return names;
	}

	@Override
	public String toString() {
		return "AFPFile [volume=" + volume + ", path=" + path + ", getParentPath()=" + getParentPath() + ", isAbsolute()="
				+ isAbsolute() + ", getName()=" + getName() + "]";
	}

	public void mkdir() {
		// TODO Auto-generated method stub
	}

	public void delete() {
		// TODO Auto-generated method stub
	}

	public void rename(AFPFile afpFile) {
		// TODO Auto-generated method stub
	}

	public AFPNodeInfo getInfo() throws IOException {
		if (this.info == null) {
			var t = volume.getClient().checkOut(false);
			try {
				this.info = t.info(volume.getId(), ROOT_NODE_ID, DEFAULT_FILE_FLAGS, DEFAULT_DIR_FLAGS, AFPConstants.MODE_EXT,
						AFPUtil.removeLeadingSlash(getAbsolutePath()));
			} finally {
				volume.getClient().checkIn(t);
			}
		}
		return this.info;
	}

	@Override
	public int getId() throws IOException {
		var info = getInfo();
		return info == null ? -1 : info.getNodeID();
	}

	@Override
	public AFPFile get(String name) throws IOException {
		return new AFPFile(this, name);
	}
}
