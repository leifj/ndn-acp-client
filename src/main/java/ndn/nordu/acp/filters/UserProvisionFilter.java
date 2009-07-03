package ndn.nordu.acp.filters;

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
import net.nordu.acp.client.ACPResult;
import net.nordu.acp.client.http.ACPHTTPClient;
import net.nordu.acp.client.pool.ACPClientFactory;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.StackObjectPool;

public class UserProvisionFilter implements Filter {

	private ObjectPool clientPool;
	private Map<String, List<String>> headerMap;
	
	private static final long DEFAULT_SESSION_TTL = 30;
	
	public void destroy() {
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest)request;
		HttpServletResponse resp = (HttpServletResponse)response;
		
		if (req.getRequestURI().endsWith("/login")) {
			ACPClient client = null;
			try {
				client = (ACPClient)clientPool.borrowObject();
				
				Map<String, String> p = new HashMap<String, String>();
				p.put("filter-login",req.getRemoteUser());
				ACPResult userResult = client.request("principal-list", p);
				if (userResult.isError()) { // create
					p.clear();
				} else { // update
					p.clear();
				}
			} catch (Exception ex) {
				try {
					clientPool.invalidateObject(client);
				} catch (Exception ignore) { }
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
		String[] headerNameArray = headers.split(",");
		List<String> result = new ArrayList<String>(headerNameArray.length);
		for (String headerName : headerNameArray) {
			result.add(headerName);
		}
		return result;
	}
	
	@SuppressWarnings("unchecked")
	public void init(FilterConfig filterConfig) throws ServletException {
		try {
			String url = filterConfig.getInitParameter("url");
			String user = filterConfig.getInitParameter("user");
			String pass = filterConfig.getInitParameter("password");
			String clientClassName = filterConfig.getInitParameter("http-client");
			String ttlStr = filterConfig.getInitParameter("session-ttl");
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
			headerMap.put("first-name",parseHeaderConfig(filterConfig,"first-name"));
			headerMap.put("last-name",parseHeaderConfig(filterConfig,"last-name"));
			headerMap.put("email",parseHeaderConfig(filterConfig, "email"));
			
			clientPool = new StackObjectPool(new ACPClientFactory(url,user,pass,httpClient,ttl));
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
	}

}
