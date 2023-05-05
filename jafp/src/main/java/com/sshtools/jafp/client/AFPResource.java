package com.sshtools.jafp.client;

import java.io.IOException;
import java.util.List;

public interface AFPResource<F extends AFPResource<?>> {
	public final static int ROOT_NODE_ID = 2;
	
	int getId() throws IOException;
	
	F get(String name) throws IOException;
	
	String getName();

	List<String> list() throws IOException;

	List<F> listFiles() throws IOException;
}
