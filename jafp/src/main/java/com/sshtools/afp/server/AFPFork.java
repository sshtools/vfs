/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.sshtools.afp.server;

import java.io.IOException;

import com.sshtools.afp.common.AFPConstants;
import com.sshtools.afp.common.ByteReader;
import com.sshtools.afp.common.ByteWriter;
import com.sshtools.afp.common.Utility;

public abstract class AFPFork extends Utility implements AFPConstants
{
	public enum Type { DATA, RESOURCE }
	
	public abstract Type getForkType()
		;

	public abstract void readRange(long offset, long length, ByteWriter ww)
		throws IOException;

	// returns the number of bytes written
	public abstract long writeRange(long offset, long length, ByteReader rr)
		throws IOException;

	public abstract boolean lockRange(long offset, long length)
		;

	public abstract boolean unlockRange(long offset, long length)
		;

	public abstract long getLength()
		throws IOException;

	public abstract void setLength(long length)
		throws IOException;

	public abstract void flush()
		throws IOException;

	public abstract void close()
		;
}

