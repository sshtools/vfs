package com.sshtools.afp.client;

import java.util.HashMap;
import java.util.Map;

public class BasicAuthenticator implements AFPAuthenticator {
	private String username;
	private char[] password;

	public BasicAuthenticator(String username, char[] password) {
		super();
		this.username = username;
		this.password = password;
	}

	@Override
	public Map<AuthDetail, char[]> authenticate(AuthDetail... require) {
		Map<AuthDetail, char[]> d = new HashMap<>();
		for (AuthDetail r : require) {
			switch (r) {
			case USERNAME:
				d.put(r, username.toCharArray());
				break;
			case PASSWORD:
				d.put(r, password);
				break;
			}
		}
		return d;
	}
}
