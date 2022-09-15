module com.sshtools.jafp {
	requires javax.jmdns;
	requires org.slf4j;
	exports com.sshtools.jafp.client;
	exports com.sshtools.jafp.common;
	exports com.sshtools.jafp.server;
}