package com.sshtools.jafp.client;

import java.util.Map;

public interface AFPAuthenticator {
	public enum AuthDetail {
		USERNAME, PASSWORD
	}
	
	Map<AuthDetail, char[]> authenticate(AuthDetail... require);
}
