package net.nordu.acp.client.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import net.nordu.acp.client.ACPClient;
import net.nordu.acp.client.ACPPrincipal;
import net.nordu.acp.client.ACPResult;
import net.nordu.acp.client.http.ACPHTTPClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ACPClientTest {

	private ACPClient client;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		try {
			String uri = System.getProperty("acp.uri", "http://localhost/api/xml");
			String user = System.getProperty("acp.user", "test");
			String password = System.getProperty("acp.password", "test");
			Class<? extends ACPHTTPClient> httpClientClass = 
				(Class<? extends ACPHTTPClient>) Class.forName(System.getProperty("acp.http.class", "net.nordu.acp.client.tests.FakeACPHTTPClient"));
			
			client = ACPClient.open(uri, user, password, httpClientClass.newInstance());
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
	
	@Test
	public void testFindPrncipal() {
		try {
			ACPResult r = client.request("principal-list-ok", null);
			assert(!r.isError());
			ACPPrincipal p = r.getPrincipal();
			assertNotNull(p);
			assertNotNull(p.getAccountId());
			System.err.println(p.getAccountId());
			assertNotNull(p.getPrincipalId());
			System.err.println(p.getPrincipalId());
			assertNotNull(p.getEmail());
			System.err.println(p.getEmail());
			assertNotNull(p.getLogin());
			System.err.println(p.getLogin());
			assertNotNull(p.getName());
			System.err.println(p.getName());
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}
}
