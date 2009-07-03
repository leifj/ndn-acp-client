package net.nordu.acp.client.tests;

import static org.junit.Assert.*;


import net.nordu.acp.client.ACPClient;
import net.nordu.acp.client.ACPResult;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ACPClientTest {

	private ACPClient client;
	
	@Before
	public void setUp() throws Exception {
		try {
			client = ACPClient.open("http://localhost/api/xml", "test", "test", new TestACPHTTPClient());
		} catch (Throwable ex) {
			ex.printStackTrace();
			client = null;
		}
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testLogin() {
		assertNotNull(client);
	}
	
	@Test
	public void testInvalid() {
		try {
			ACPResult r = client.request("test1-invalid", null);
			assert(r.isError());
			assert(r.getError().contains("invalid"));
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}
}
