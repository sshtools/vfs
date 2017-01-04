/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.sshtools.afp.server;

import java.io.IOException;

@SuppressWarnings("serial")
class AFPError extends IOException
{
	private int error;

	AFPError(int error)
	{
		this.error = error;
	}

	public int getError()
	{
		return error;
	}
	
	public String toString() {
		return "AFPError:"+error;
	}
}

