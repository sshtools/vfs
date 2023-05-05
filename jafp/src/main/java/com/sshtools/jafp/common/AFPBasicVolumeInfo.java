/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */
package com.sshtools.jafp.common;

import java.io.IOException;

public class AFPBasicVolumeInfo extends Utility {
	private boolean passwordProtected;
	private boolean hasUnixPrivs;
	private String name;

	public AFPBasicVolumeInfo(String name, boolean passwordProtected, boolean hasUnixPrivs) {
		this.name = name;
		this.hasUnixPrivs = hasUnixPrivs;
		this.passwordProtected = passwordProtected;
	}

	public AFPBasicVolumeInfo(ByteReader rr) throws IOException {
		int b = rr.readUnsignedByte();
		hasUnixPrivs = ( b & 0x1 ) != 0;
		passwordProtected = ( b & 0x80 ) != 0;
		name = rr.readPString();
	}

	public void write(ByteWriter ww) {
		ww.writeByte(((hasUnixPrivs ? 0x1 : 0) | (passwordProtected ? 0x80 : 0)));
		ww.writePString(name);
	}

	public boolean isPasswordProtected() {
		return passwordProtected;
	}

	public boolean isHasUnixPrivs() {
		return hasUnixPrivs;
	}

	public String getName() {
		return name;
	}

	public String toString() {
		return "name=" + name + ",passwordProtected=" + passwordProtected + ",hasUnixPrivs=" + hasUnixPrivs;
	}
}
