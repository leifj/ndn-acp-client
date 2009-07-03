package net.nordu.acp.client;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.nordu.acp.client.http.ACPHTTPClient;
import net.nordu.acp.client.http.SimpleHTTPClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ACPClient {

	private Log log = LogFactory.getLog(ACPClient.class);
	
	private String apiUrl;
	private String session;
	private ACPHTTPClient httpClient;
	private long createTime;
	
	public ACPHTTPClient getHttpClient() {
		return httpClient;
	}
	
	public void setHttpClient(ACPHTTPClient httpClient) {
		this.httpClient = httpClient;
	}
	
	protected ACPClient(String apiUrl, ACPHTTPClient httpClient) {
		this.apiUrl = apiUrl;
		this.httpClient = httpClient;
		this.createTime = (new Date()).getTime();
	}
	
	protected ACPClient(String apiUrl) throws ACPException {
		this(apiUrl,new SimpleHTTPClient());
	}
	
	private String getCookie() throws ACPException {
		return isInSession() ? "BREEZESESSION="+session : null;
	}
	
	private boolean isInSession() {
		return session != null;
	}
	
	public boolean isExpired(long ttl) {
		Date now = new Date();
		return now.getTime() > createTime + ttl;
	}
	
	public long getCreateTime() {
		return createTime;
	}
	
	protected void login(String user, String password) throws ACPException {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("login", user);
		parameters.put("password",password);
		ACPResult result = request("login",parameters);
		if (result.isError())
			throw new ACPAuthenticationException(result.getError());
	}
	
	public static ACPClient open(String apiUrl, String user, String password, ACPHTTPClient httpClient) throws ACPException {
		ACPClient client = new ACPClient(apiUrl,httpClient);
		
		client.login(user,password);
		
		return client;
	}
	
	public ACPResult request(String action, Map<String, String> p) throws ACPException {
		try {
			StringBuffer url = new StringBuffer();
			url.append(apiUrl).append("?").append("action=").append(action);
			if (p != null && p.size() > 0) {
				for (Map.Entry<String, String> e : p.entrySet()) {
					url.append("&"); 
					url.append(e.getKey()).append("=").append(e.getValue());
				}
			}
			System.err.println("httpClient="+httpClient);
			System.err.println("url="+url);
			ACPResult r = ACPResult.parse(httpClient.GET(url.toString(),getCookie()));
			log.info(r);
			System.err.println(r);
			return r;
		} catch (Exception ex) {
			throw new ACPException(ex);
		}
	}

	public ACPPrincipal findOrCreatePrincipal(String key, String value, String type, Map<String,String> properties) throws ACPException {
		Map<String, String> request = new HashMap<String, String>();
		request.put("filter-"+key,value);
		request.put("filter-type", type);
		ACPResult userResult = request("principal-list", request);
		ACPPrincipal principal = userResult.getPrincipal();
		if (userResult.isError()) {
			// unless this is a "normal" error of no-data throw an exception 
			if (!userResult.getStatusCode().equals("no-data"))
				throw userResult.getException();
		} else if (principal != null) {
			properties.put("principal-id", principal.getPrincipalId());
		}
		
		// remove null or empty values - it really confuses ACP
		for (Map.Entry<String, String> e : properties.entrySet()) {
			if (e.getValue() == null || e.getValue().length() == 0)
				properties.remove(e.getKey());
		}
		
		ACPResult updateResult = request("principal-update", properties);
		if (updateResult.isError()) {
			throw updateResult.getException();
		}
		
		ACPPrincipal returnPrincipal = updateResult.getPrincipal();
		if (returnPrincipal == null)
			returnPrincipal = principal;
		
		return returnPrincipal;
	}
	
	public ACPPrincipal findBuiltIn(String type) throws ACPException {
		Map<String, String> request = new HashMap<String, String>();
		request.put("filter-type", type);
		ACPResult userResult = request("principal-list", request);
		ACPPrincipal principal = userResult.getPrincipal();
		if (userResult.isError())
			throw userResult.getException();
		
		return principal;
	}
	
	public void addRemoveMember(String principalId, String groupId, boolean isMember) throws ACPException {
		Map<String, String> request = new HashMap<String, String>();
		request.put("group-id",groupId);
		request.put("principal-id", principalId);
		request.put("is-member",Boolean.toString(isMember));
		ACPResult userResult = request("group-membership-update", request);
		if (userResult.isError())
			throw userResult.getException();
	}
	
	public void addMember(String principalId, String groupId) throws ACPException {
		addRemoveMember(principalId, groupId, true);
	}
	
	public void removeMember(String principalId, String groupId) throws ACPException {
		addRemoveMember(principalId, groupId, false);
	}
	
}
