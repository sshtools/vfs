/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */
package com.sshtools.afp.common;

import java.io.IOException;

import com.sshtools.afp.server.AFPCNode;
import com.sshtools.afp.server.AFPNodeInfo;

public class AFPFileInfo extends AFPNodeInfo {
	private long resourceForkLen;
	private int launchLimit;
	private long dataForkLen;
	private int shortResourceForkLen;
	private int shortDataForkLen;

	public AFPFileInfo(int flags, AFPCNode node) {
		this.flags = flags;
		attributes = node.getAttributes();
		parentNodeID = node.getParentNodeID();
		createDate = node.getCreateDate();
		modifiedDate = node.getModifiedDate();
		backupDate = node.getBackupDate();
		finderInfo = node.getFinderInfo();
		longName = node.getLongName();
		shortName = node.getShortName();
		nodeID = node.getNodeID();
		utf8Name = node.getUTF8Name();
		shortDataForkLen = node.getShortDataForkLen();
		shortResourceForkLen = node.getShortResourceForkLen();
		dataForkLen = node.getDataForkLen();
		launchLimit = node.getLaunchLimit();
		resourceForkLen = node.getResourceForkLen();
		unixPrivs = node.getUnixPrivs();
	}

	public AFPFileInfo(int flags, ByteReader rr) throws IOException {
		rr.markDeferredOffset();
		finderInfo = new byte[16];
		unixPrivs = new byte[16];
		this.flags = flags;
		if (hasBits(flags, AFPConstants.FILE_BIT_ATTRIBUTE)) {
			attributes = rr.readShort();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_PARENT_DIR_ID)) {
			parentNodeID = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_CREATE_DATE)) {
			createDate = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_MOD_DATE)) {
			modifiedDate = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_BACKUP_DATE)) {
			backupDate = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_FINDER_INFO)) {
			try {
			rr.readBytes(finderInfo);
			}
			catch(ArrayIndexOutOfBoundsException a) {
				throw a;
			}
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_LONG_NAME)) {
			longName = rr.readPStringDeferred();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_SHORT_NAME)) {
			shortName = rr.readPStringDeferred();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_NODE_ID)) {
			nodeID = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_DATA_FORK_LEN)) {
			shortDataForkLen = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_RSRC_FORK_LEN)) {
			shortResourceForkLen = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_XDATA_FORK_LEN)) {
			dataForkLen = rr.readLong();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_LAUNCH_LIMIT)) {
			launchLimit = rr.readShort();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_UTF8_NAME)) {
			utf8Name = rr.readPStringDeferred();
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_XRSRC_FORK_LEN)) {
			resourceForkLen = rr.readLong();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_UNIX_PRIVS)) {
			rr.readBytes(unixPrivs);
		}
	}

	public void write(ByteWriter ww) {
		ww.markDeferredOffset();
		if (hasBits(flags, AFPConstants.FILE_BIT_ATTRIBUTE)) {
			ww.writeShort(attributes);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_PARENT_DIR_ID)) {
			ww.writeInt(parentNodeID);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_CREATE_DATE)) {
			ww.writeInt(createDate);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_MOD_DATE)) {
			ww.writeInt(modifiedDate);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_BACKUP_DATE)) {
			ww.writeInt(backupDate);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_FINDER_INFO)) {
			ww.writeBytes(finderInfo == null ? new byte[16] : finderInfo);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_LONG_NAME)) {
			ww.writePStringDeferred(longName);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_SHORT_NAME)) {
			ww.writePStringDeferred(shortName);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_NODE_ID)) {
			ww.writeInt(nodeID);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_DATA_FORK_LEN)) {
			ww.writeInt(shortDataForkLen);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_RSRC_FORK_LEN)) {
			ww.writeInt(shortResourceForkLen);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_XDATA_FORK_LEN)) {
			ww.writeLong(dataForkLen);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_LAUNCH_LIMIT)) {
			ww.writeShort(launchLimit);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_UTF8_NAME)) {
			ww.writeAFPStringDeferred(utf8Name);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_XRSRC_FORK_LEN)) {
			ww.writeLong(resourceForkLen);
		}
		if (hasBits(flags, AFPConstants.FILE_BIT_UNIX_PRIVS)) {
			ww.writeBytes(unixPrivs == null ? new byte[16] : unixPrivs);
		}
	}

	public long getResourceForkLen() {
		return resourceForkLen;
	}

	public int getLaunchLimit() {
		return launchLimit;
	}

	public long getDataForkLen() {
		return dataForkLen;
	}

	public int getShortResourceForkLen() {
		return shortResourceForkLen;
	}

	public int getShortDataForkLen() {
		return shortDataForkLen;
	}
}
