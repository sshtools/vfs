package com.sshtools.jafp.server;

import com.sshtools.jafp.common.Utility;

public abstract class AFPNodeInfo extends Utility {
	protected int flags;
	protected int attributes;
	protected int parentNodeID;
	protected int createDate;
	protected int modifiedDate;
	protected int backupDate;
	protected String shortName;
	protected String longName;
	protected byte[] finderInfo;
	protected int nodeID;
	protected String utf8Name;
	protected byte[] unixPrivs;

	public int getFlags() {
		return flags;
	}

	public int getAttributes() {
		return attributes;
	}

	public int getParentNodeID() {
		return parentNodeID;
	}

	public int getCreateDate() {
		return createDate;
	}

	public int getModifiedDate() {
		return modifiedDate;
	}

	public int getBackupDate() {
		return backupDate;
	}

	public String getShortName() {
		return shortName;
	}

	public String getLongName() {
		return longName;
	}

	public byte[] getFinderInfo() {
		return finderInfo;
	}

	public int getNodeID() {
		return nodeID;
	}

	public String getUtf8Name() {
		return utf8Name;
	}

	public byte[] getUnixPrivs() {
		return unixPrivs;
	}
}
