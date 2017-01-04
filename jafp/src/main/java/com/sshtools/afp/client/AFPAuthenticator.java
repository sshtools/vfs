package com.sshtools.afp.client;

import java.util.Map;

public interface AFPAuthenticator {
	public enum AuthDetail {
		USERNAME, PASSWORD
	}
	
	Map<AuthDetail, char[]> authenticate(AuthDetail... require);
}
