/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */
package com.sshtools.jafp.server;

import java.io.IOException;
import java.util.Arrays;

import com.sshtools.jafp.common.AFPBasicVolumeInfo;
import com.sshtools.jafp.common.ByteReader;
import com.sshtools.jafp.common.ByteWriter;
import com.sshtools.jafp.common.Utility;

// Reference: Inside Appletalk p. 442
public class AFPServerParams extends Utility {
	private AFPBasicVolumeInfo[] vols;
	private long time;
	
	static AFPBasicVolumeInfo[] volumeInfoForVolumes(AFPServerVolume[] volumes) {
		int i = 0;
		AFPBasicVolumeInfo[] info = new AFPBasicVolumeInfo[volumes.length];
		for(AFPServerVolume v : volumes) {
			info[i++] = v.getInfo();
		}
		return info;
	}

	public AFPServerParams(AFPServerVolume[] vols) {
		this(volumeInfoForVolumes(vols));
	}
	
	public AFPServerParams(AFPBasicVolumeInfo[] vols) {
		this.vols = vols;
		time = System.currentTimeMillis();
	}

	public AFPServerParams(ByteReader rr) throws IOException {
		time = afp2unixTime(rr.readInt());
		vols = new AFPBasicVolumeInfo[rr.readUnsignedByte()];
		for (int i = 0; i < vols.length; i++) {
			vols[i] = new AFPBasicVolumeInfo(rr);
		}
	}

	public AFPBasicVolumeInfo[] getVols() {
		return vols;
	}

	public long getTime() {
		return time;
	}

	public void write(ByteWriter ww) {
		ww.writeInt(unix2afpTime(System.currentTimeMillis()));
		ww.writeByte(vols.length);
		for (int i = 0; i < vols.length; i++) {
			vols[i].write(ww);
		}
	}

	@Override
	public String toString() {
		return "AFP_ServerParams [vols=" + Arrays.toString(vols) + "]";
	}
}
