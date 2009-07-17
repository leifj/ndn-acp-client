package net.nordu.acp.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.nordu.acp.client.ACPClient;
import net.nordu.acp.client.ACPException;
import net.nordu.acp.client.ACPPrincipal;
import net.nordu.acp.client.ACPResult;
import net.nordu.acp.client.http.ACPHTTPClient;
import net.nordu.acp.client.pool.ACPClientFactory;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

public class UserProvisionFilter implements Filter {

	private ObjectPool clientPool;
	private Map<String, List<String>> headerMap;
	
	public Map<String, List<String>> getHeaderMap() {
		return headerMap;
	}
	
	private static final long DEFAULT_SESSION_TTL = 30;
	
	public void destroy() {
	}
	
	public ObjectPool getClientPool() {
		return clientPool;
	}

	public String getProperty(HttpServletRequest req, String name) {
		List<String> headerNames = headerMap.get(name);
		if (headerNames == null)
			return req.getHeader(name);
		
		for (String hn : headerNames) {
			String value = req.getHeader(hn);
			if (!isNullOrEmpty(value))
				return value;
		}
		
		return null;
	}
	
	public boolean hasProperty(HttpServletRequest req, String name) {
		String v = getProperty(req, name);
		return !isNullOrEmpty(v);
	}
	
	private boolean isMemberOrEmployee(String[] affiliations) {
		for (String affiliation : affiliations) {
			if (affiliation.contains("@")) {
			   String parts[] = affiliation.split("@");
			   System.err.println("aff: "+parts[0]+"@"+parts[1]);
			   System.err.println("aff: "+parts[0]);
			   if (parts[0].equalsIgnoreCase("member"))
				return true;
			   if (parts[0].equalsIgnoreCase("employee"))
				return true;
                        }
		}
		return false;
	}

	private boolean isNullOrEmpty(String x) {
		return x == null || x.length() == 0 || x.equals("(null)");
 	}
	
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		final HttpServletRequest req = (HttpServletRequest)request;
		final HttpServletResponse resp = (HttpServletResponse)response;
		final String uid = req.getHeader("X_REMOTE_USER");
		System.err.println("request uri="+req.getRequestURI());
		System.err.println("uid="+uid);
		if (req.getRequestURI().contains("/system/login") && !isNullOrEmpty(uid)) {
			ACPClient client = null;
			try {
				System.err.println("in doFilter");
			 	
				if (!uid.contains("@")) {
					resp.sendRedirect("https://connect-test.sunet.se/errors/missing-attribute.php?attribute=eduPersonPrincipalName");
					return;
				}
				
				if (!hasProperty(req,"first-name")) {
					resp.sendRedirect("https://connect-test.sunet.se/errors/missing-attribute.php?attribute=givenName");
					return;
				}
				
				if (!hasProperty(req,"last-name")) {
					resp.sendRedirect("https://connect-test.sunet.se/errors/missing-attribute.php?attribute=sn");
					return;
				}
				/*	
				if (!hasProperty(req,"email")) {
					resp.sendRedirect("https://connect-test.sunet.se/errors/missing-attribute.php?attribute=mail");
					return;
				}
				*/
				
				client = (ACPClient)clientPool.borrowObject();
				
				ACPPrincipal user = 
					client.findOrCreatePrincipal("login", uid, "user", new HashMap<String, String>() {{
					put("type", "user");
					put("has-children", "0");
					put("first-name", getProperty(req,"first-name"));
					put("last-name", getProperty(req,"last-name"));
					put("email", getProperty(req,"email"));
					put("login",uid);
					put("ext-login", uid);
				}});
				
				System.err.println("principal="+user.getPrincipalId());
				String affiliations = getProperty(req,"affiliation");
				System.err.println("affiliation="+affiliations);
				
				if (!isNullOrEmpty(affiliations) && isMemberOrEmployee(affiliations.split(";"))) {
					System.err.println("add live-admins");
					ACPPrincipal liveAdmins = client.findBuiltIn("live-admins");
					if (liveAdmins != null)
						client.addMember(user.getPrincipalId(), liveAdmins.getPrincipalId());
					ACPPrincipal seminarAdmins = client.findBuiltIn("seminar-admins");
					if (seminarAdmins != null)
						client.addMember(user.getPrincipalId(), seminarAdmins.getPrincipalId());
				} else {
                                        System.err.println("remove live-admins");
					ACPPrincipal liveAdmins = client.findBuiltIn("live-admins");
					if (liveAdmins != null)
						client.removeMember(user.getPrincipalId(), liveAdmins.getPrincipalId());
					ACPPrincipal seminarAdmins = client.findBuiltIn("seminar-admins");
					if (seminarAdmins != null)
						client.removeMember(user.getPrincipalId(), seminarAdmins.getPrincipalId());
				}
			} catch (Exception ex) {
				try {
					clientPool.invalidateObject(client);
				} catch (Exception ignore) { }
				//TODO redirect to sensible error page for certain types of errors
				throw new ServletException(ex);
			} finally {
				try { 
					if (client != null)
						clientPool.returnObject(client);
				} catch (Exception ignore) { }
			}
		}
		
		chain.doFilter(request, response);
	}

	private List<String> parseHeaderConfig(FilterConfig filterConfig, String pn) {
		String headers = filterConfig.getInitParameter(pn);
		List<String> result = new ArrayList<String>();
		if (headers != null) {
			String[] headerNameArray = headers.split(",");
			for (String headerName : headerNameArray) {
				result.add(headerName);
			}
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public void init(FilterConfig filterConfig) throws ServletException {
		try {
			String url = filterConfig.getInitParameter("acp.url");
			String user = filterConfig.getInitParameter("acp.user");
			String pass = filterConfig.getInitParameter("acp.password");
			String clientClassName = filterConfig.getInitParameter("acp.http-client");
			String ttlStr = filterConfig.getInitParameter("acp.session-ttl");
			long ttl = DEFAULT_SESSION_TTL;
			if (ttlStr != null) {
				try {
					Long t = Long.parseLong(ttlStr);
					ttl = t.longValue();
				} catch (NumberFormatException ex) {
					ex.printStackTrace();
				}
			}
			
			ACPHTTPClient httpClient = null;
			if (clientClassName != null) {
				Class<ACPHTTPClient> clientClass = (Class<ACPHTTPClient>)Class.forName(clientClassName);
				httpClient = clientClass.newInstance();
			}
			headerMap = new HashMap<String, List<String>>();
			headerMap.put("first-name",parseHeaderConfig(filterConfig,"acp.header-map.first-name"));
			headerMap.put("last-name",parseHeaderConfig(filterConfig,"acp.header-map.last-name"));
			headerMap.put("email",parseHeaderConfig(filterConfig, "acp.header-map.email"));
			
			clientPool = new StackObjectPool(new ACPClientFactory(url,user,pass,httpClient,ttl));
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
	}

}
