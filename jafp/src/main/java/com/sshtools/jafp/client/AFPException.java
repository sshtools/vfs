package com.sshtools.jafp.client;

import java.io.IOException;

@SuppressWarnings("serial")
public class AFPException extends IOException {
	public enum Error {
		AUTHENTICATION_CANCELLED, REQUIRE_AUTHENTICATION, SERVER_ERROR, AUTHENTICATION_FAILED, PASSWORD_TOO_LONG_FOR_AUTHENTICATION_TYPE,
	}

	private Error error;
	private int code;

	public AFPException(Error error) {
		this(error, error.name());
	}

	public AFPException(Error error, String message, Throwable cause) {
		super(message, cause);
		this.error = error;
	}

	public AFPException(Error error, String message) {
		super(message);
		this.error = error;
	}

	public AFPException(Error error, Throwable cause) {
		super(cause);
		this.error = error;
	}

	public Error getError() {
		return error;
	}

	public int getCode() {
		return code;
	}

	public AFPException setCode(int code) {
		this.code = code;
		return this;
	}
}
