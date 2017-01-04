/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */
package com.sshtools.afp.common;

import java.io.IOException;

import com.sshtools.afp.server.AFPCNode;
import com.sshtools.afp.server.AFPNodeInfo;

public class AFPDirectoryInfo extends AFPNodeInfo {
	private int offspringCount;
	private int ownerID;
	private int accessRights;
	private int groupID;

	public AFPDirectoryInfo(int flags, AFPCNode node) {
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
		offspringCount = node.countOffspring();
		ownerID = node.getOwnerID();
		groupID = node.getGroupID();
		accessRights = node.getAccessRights();
		utf8Name = node.getUTF8Name();
		unixPrivs = node.getUnixPrivs();
	}

	public AFPDirectoryInfo(int flags, ByteReader rr) throws IOException {
		rr.markDeferredOffset();
		finderInfo = new byte[16];
		unixPrivs = new byte[16];
		this.flags = flags;
		if (hasBits(flags, AFPConstants.DIR_BIT_ATTRIBUTE)) {
			attributes = rr.readShort();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_PARENT_DIR_ID)) {
			parentNodeID = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_CREATE_DATE)) {
			createDate = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_MOD_DATE)) {
			modifiedDate = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_BACKUP_DATE)) {
			backupDate = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_FINDER_INFO)) {
			rr.readBytes(finderInfo);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_LONG_NAME)) {
			longName = rr.readPStringDeferred();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_SHORT_NAME)) {
			shortName = rr.readPStringDeferred();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_NODE_ID)) {
			nodeID = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_OFFSPRING_COUNT)) {
			offspringCount = rr.readShort();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_OWNER_ID)) {
			ownerID = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_GROUP_ID)) {
			groupID = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_ACCESS_RIGHTS)) {
			accessRights = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_UTF8_NAME)) {
			utf8Name = rr.readAFPStringDeferred();
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_UNIX_PRIVS)) {
			rr.readBytes(unixPrivs);
		}
	}

	public void write(ByteWriter ww) {
		ww.markDeferredOffset();
		if (hasBits(flags, AFPConstants.DIR_BIT_ATTRIBUTE)) {
			ww.writeShort(attributes);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_PARENT_DIR_ID)) {
			ww.writeInt(parentNodeID);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_CREATE_DATE)) {
			ww.writeInt(createDate);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_MOD_DATE)) {
			ww.writeInt(modifiedDate);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_BACKUP_DATE)) {
			ww.writeInt(backupDate);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_FINDER_INFO)) {
			ww.writeBytes(finderInfo);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_LONG_NAME)) {
			ww.writePStringDeferred(longName);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_SHORT_NAME)) {
			ww.writePStringDeferred(shortName);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_NODE_ID)) {
			ww.writeInt(nodeID);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_OFFSPRING_COUNT)) {
			ww.writeShort(offspringCount);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_OWNER_ID)) {
			ww.writeInt(ownerID);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_GROUP_ID)) {
			ww.writeInt(groupID);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_ACCESS_RIGHTS)) {
			ww.writeInt(accessRights);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_UTF8_NAME)) {
			ww.writeAFPStringDeferred(utf8Name);
		}
		if (hasBits(flags, AFPConstants.DIR_BIT_UNIX_PRIVS)) {
			ww.writeBytes(unixPrivs);
		}
	}

	public int getOffspringCount() {
		return offspringCount;
	}

	public int getOwnerID() {
		return ownerID;
	}

	public int getAccessRights() {
		return accessRights;
	}

	public int getGroupID() {
		return groupID;
	}
}
