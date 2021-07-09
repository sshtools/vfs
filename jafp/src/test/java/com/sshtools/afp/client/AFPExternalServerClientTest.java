package com.sshtools.afp.client;

import java.io.InputStream;
import java.util.Properties;

import org.junit.Before;

import com.sshtools.afp.common.AFPConstants;

public class AFPExternalServerClientTest extends AbstractEmbeddedServerTest {
	Properties properties = new Properties();

	@Before
	public void setup() throws Exception {
		try (InputStream in = getClass().getResourceAsStream("/test.properties")) {
			properties.load(in);
		}
	}

//	@Test
	public void volumeList() throws Exception {
		AFPClient c = new AFPClient(properties.getProperty("testHost"), Integer.parseInt(properties.getProperty( "testPort", "548")),
				properties.getProperty("testUser"), properties.getProperty("testPassword").toCharArray());
		c.setAuthenticationMethods(AFPConstants.UAM_STR_DHX_128);
		// c.list();
		for (AFPVolume vol : c.list()) {
			System.out.println(vol);
			// // AFPFile dsstore = vol.get("._.DS_Store");
			// AFPFile dsstore = vol.get("Spark.exe");
			// System.out.println(dsstore);
			// for (AFPVolume v : c.list()) {
			// System.out.println(">> " + v);
			// for (String z : v.list()) {
			// System.out.println("-- " + z);
			// }
			// dump(v, 0);
			// }
		}
	}
}
