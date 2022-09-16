module com.sshtools.jafp {
	requires transitive javax.jmdns;
	requires transitive org.slf4j;
	exports com.sshtools.jafp.client;
	exports com.sshtools.jafp.common;
	exports com.sshtools.jafp.server;
}