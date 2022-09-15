open module com.sshtools.vfs.googledrive {
	requires commons.vfs2;
	requires commons.logging;
	requires transitive com.google.api.client;
	requires google.api.services.drive.v3.rev197;
	requires transitive com.google.api.client.json.jackson2;
	requires transitive google.api.client;
	requires google.oauth.client;
	requires transitive com.google.api.client.extensions.java6.auth;
	requires com.google.api.client.extensions.jetty.auth;
}
