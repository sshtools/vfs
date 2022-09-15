open module com.sshtools.vfs.gcs {
	requires commons.vfs2;
	requires commons.logging;
	requires google.cloud.storage;
	requires google.cloud.core;
	requires transitive gax;
	requires com.google.auth.oauth2;
	requires com.google.api.client.auth;
	requires com.google.auth;
}
