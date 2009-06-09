package net.nordu.acp.client.pool;

import net.nordu.acp.client.ACPClient;
import net.nordu.acp.client.ACPResult;
import net.nordu.acp.client.http.ACPHTTPClient;

import org.apache.commons.pool.BasePoolableObjectFactory;

public class ACPClientFactory extends BasePoolableObjectFactory {

	private String apiUrl;
	private String user;
	private String password;
	private ACPHTTPClient httpClient;
	private long ttl;
	
	public ACPClientFactory(String apiUrl, String user, String password, ACPHTTPClient httpClient, long ttl) {
		this.apiUrl = apiUrl;
		this.user = user;
		this.password = password;
		this.httpClient = httpClient;
		this.ttl = ttl;
	}
	
	@Override
	public Object makeObject() throws Exception {
		return ACPClient.open(apiUrl, user, password, httpClient);
	}
	
	@Override
	public boolean validateObject(Object obj) {
		ACPClient client = (ACPClient)obj;
		try {
			if (client.isExpired(ttl))
				return false;
			
			ACPResult r = client.request("common-info", null);
			if (r == null || r.getDocument() == null)
				return false;
			
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
}
