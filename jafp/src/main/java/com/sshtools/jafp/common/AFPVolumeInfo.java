/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */
package com.sshtools.jafp.common;

import java.io.IOException;

import com.sshtools.jafp.server.AFPServerVolume;

public class AFPVolumeInfo extends Utility {
	private int flags;
	private int attributes;
	private int createDate;
	private int modifiedDate;
	private int backupDate;
	private int signature;
	private int id;
	private int bytesFree;
	private int bytesTotal;
	private long extBytesFree;
	private long extBytesTotal;
	private int blockSize;
	private String name;

	public AFPVolumeInfo(int flags, AFPServerVolume vol) {
		this.flags = flags;
		attributes = vol.getAttributes();
		signature = vol.getSignature();
		createDate = vol.getCreateDate();
		modifiedDate = vol.getModifiedDate();
		backupDate = vol.getBackupDate();
		id = vol.getID();
		bytesFree = vol.getBytesFree();
		bytesTotal = vol.getBytesTotal();
		name = vol.getName();
		extBytesFree = vol.getExtBytesFree();
		extBytesTotal = vol.getExtBytesTotal();
		blockSize = vol.getBlockSize();
	}

	public AFPVolumeInfo(ByteReader rr) throws IOException {
		this.flags = rr.readShort();
		if (hasBits(flags, AFPConstants.VOL_BIT_ATTRIBUTE)) {
			attributes = rr.readShort();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_SIGNATURE)) {
			signature = rr.readShort();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_CREATE_DATE)) {
			createDate = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_MOD_DATE)) {
			modifiedDate = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_BACKUP_DATE)) {
			backupDate = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_ID)) {
			id = rr.readShort();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_BYTES_FREE)) {
			bytesFree = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_BYTES_TOTAL)) {
			bytesTotal = rr.readInt();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_NAME)) {
			name = rr.readPString();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_XBYTES_FREE)) {
			extBytesFree = rr.readLong();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_XBYTES_TOTAL)) {
			extBytesTotal = rr.readLong();
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_BLOCK_SIZE)) {
			blockSize = rr.readInt();
		}
	}

	public void write(ByteWriter ww) {
		ww.writeShort(flags);
		ww.markDeferredOffset();
		if (hasBits(flags, AFPConstants.VOL_BIT_ATTRIBUTE)) {
			ww.writeShort(attributes);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_SIGNATURE)) {
			ww.writeShort(signature);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_CREATE_DATE)) {
			ww.writeInt(createDate);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_MOD_DATE)) {
			ww.writeInt(modifiedDate);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_BACKUP_DATE)) {
			ww.writeInt(backupDate);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_ID)) {
			ww.writeShort(id);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_BYTES_FREE)) {
			ww.writeInt(bytesFree);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_BYTES_TOTAL)) {
			ww.writeInt(bytesTotal);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_NAME)) {
			ww.writePStringDeferred(name);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_XBYTES_FREE)) {
			ww.writeLong(extBytesFree);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_XBYTES_TOTAL)) {
			ww.writeLong(extBytesTotal);
		}
		if (hasBits(flags, AFPConstants.VOL_BIT_BLOCK_SIZE)) {
			ww.writeInt(blockSize);
		}
	}

	public int getFlags() {
		return flags;
	}

	public int getAttributes() {
		return attributes;
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

	public int getSignature() {
		return signature;
	}

	public int getId() {
		return id;
	}

	public int getBytesFree() {
		return bytesFree;
	}

	public int getBytesTotal() {
		return bytesTotal;
	}

	public long getExtBytesFree() {
		return extBytesFree;
	}

	public long getExtBytesTotal() {
		return extBytesTotal;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public String getName() {
		return name;
	}
}
