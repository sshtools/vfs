/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */
package com.sshtools.afp.common;

import java.io.IOException;

// Reference: Inside Appletalk p. 442
// TODO: icon/mask support
public class AFPServerInfo extends Utility {
	private String serverName;
	private String afpVersions[];
	private String uamModules[];
	private Object icon;
	private int flags;

	public AFPServerInfo(String sn, String av[], String ua[], int flags) {
		this.serverName = sn;
		this.afpVersions = av;
		this.uamModules = ua;
		this.flags = flags;
	}

	public AFPServerInfo(ByteReader rr) throws IOException {
		serverName = readPString(rr, readInt2(rr, 0));
		afpVersions = readPStringArray(rr, readInt2(rr, 2));
		uamModules = readPStringArray(rr, readInt2(rr, 4));
		flags = readInt2(rr, 8);
	}

	public String getServerName() {
		return serverName;
	}

	public String[] getAfpVersions() {
		return afpVersions;
	}

	public String[] getUamModules() {
		return uamModules;
	}

	public Object getIcon() {
		return icon;
	}

	public int getFlags() {
		return flags;
	}

	private int readInt2(ByteReader rr, int pos) throws IOException {
		rr.seek(pos);
		return rr.readUnsignedShort();
	}

	private String readPString(ByteReader rr, int pos) throws IOException {
		rr.seek(pos);
		return rr.readPString();
	}

	private String[] readPStringArray(ByteReader rr, int pos) throws IOException {
		rr.seek(pos);
		return rr.readPStringArray();
	}

	public void write(ByteWriter ww) {
		ww.writePStringDeferred("Jaffer");
		ww.writePStringArrayDeferred(afpVersions);
		ww.writePStringArrayDeferred(uamModules);
		ww.writeShort(0); // icon/mask offset
		ww.writeShort(flags);
		ww.writePString(serverName); // server name
		if (ww.getOffset() % 2 == 1) {
			ww.writeByte(0);
		}
		ww.writeShort(0); // server signature offset (not implemented)
		ww.writeShort(0); // network address offset (TODO)
		ww.writeShort(0); // directory names offset (not implemented)
		// ww.writePString(serverName); // server name in UTF8 (TODO)
	}

	public String toString() {
		return "sn=" + serverName + ",av=" + list(afpVersions) + ",uam=" + list(uamModules) + ",fl=" + bits(flags, 2);
	}
}
