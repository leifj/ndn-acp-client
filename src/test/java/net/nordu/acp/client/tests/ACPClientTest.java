package net.nordu.acp.client.tests;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import net.nordu.acp.client.ACPClient;
import net.nordu.acp.client.ACPException;
import net.nordu.acp.client.ACPResult;
import net.nordu.acp.client.http.ACPHTTPClient;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ACPClientTest {

	private ACPClient client;
	
	private InputStream load(String str) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream("client-tests/"+str+".xml");
	}
	
	@Before
	public void setUp() throws Exception {
		try {
			client = ACPClient.open("http://localhost/api/xml", "test", "test", new ACPHTTPClient() {
				public InputStream GET(String url, String cookie) throws ACPException {
					try {
						URL u = new URL(url);
						String q = u.getQuery();
						Map<String, String> params = new HashMap<String, String>();
						String[] qs = q.split("&");
						for (String a : qs) {
							String[] av = a.split("=");
							params.put(av[0], av[1]);
						}
						
						String action = params.get("action");
						if (action.equals("login")) {
							return load("login-ok");
						}
						
						return load(action);
					} catch (Exception ex) {
						ex.printStackTrace();
						throw new ACPException(ex);
					}
				}
			});
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
