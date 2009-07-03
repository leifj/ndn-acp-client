/**
 * 
 */
package net.nordu.acp.client.tests;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;

import net.nordu.acp.client.ACPException;
import net.nordu.acp.client.http.ACPHTTPClient;

@Ignore
public class FakeACPHTTPClient implements ACPHTTPClient {

	public FakeACPHTTPClient() {
	}

	private InputStream load(String str) {
		return Thread.currentThread().getContextClassLoader().getResourceAsStream("client-tests/"+str+".xml");
	}

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
}