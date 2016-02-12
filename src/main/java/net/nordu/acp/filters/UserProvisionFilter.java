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
import net.nordu.acp.client.http.ACPHTTPClient;
import net.nordu.acp.utils.Cache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class UserProvisionFilter implements Filter {

        private Log log = LogFactory.getLog(UserProvisionFilter.class);

	private Map<String, List<String>> headerMap;

	public Map<String, List<String>> getHeaderMap() {
		return headerMap;
	}

	private static final long DEFAULT_SESSION_TTL = 30;
        private static final int DEFAULT_CACHE_TTL = 5*60;

	public void destroy() {
	}

	private String firstValueOf(String v) throws ACPException {
		String[] va = v.split(";");
		return va[0];
	}

	public String getProperty(HttpServletRequest req, String name)
			throws ACPException {
		List<String> headerNames = headerMap.get(name);
		if (headerNames == null)
			return req.getHeader(name);

		for (String hn : headerNames) {
			String value = req.getHeader(hn);
			if (!isNullOrEmpty(value))
				return firstValueOf(value);
		}

		return null;
	}

	public boolean hasProperty(HttpServletRequest req, String name)
			throws ACPException {
		String v = getProperty(req, name);
		return !isNullOrEmpty(v);
	}

	private boolean isMemberOrEmployee(String[] affiliations) {
		boolean student = false;
		boolean member = false;
		boolean staff = false;
		boolean employee = false;
                boolean faculty = false;
		for (String affiliation : affiliations) {
			if (affiliation.contains("@")) {
				String parts[] = affiliation.split("@");
				System.err.println("aff: " + parts[0] + "@" + parts[1]);
				System.err.println("aff: " + parts[0]);
				if (parts[0].equalsIgnoreCase("member"))
					member = true;
				else if (parts[0].equalsIgnoreCase("employee"))
					employee = true;
				else if (parts[0].equalsIgnoreCase("staff"))
					staff = true;
				else if (parts[0].equalsIgnoreCase("student"))
					student = true;
                                else if (parts[0].equalsIgnoreCase("faculty"))
                                        faculty = true;
			}
		}
		return employee || staff || faculty || (member && !student);
	}

        private String domain(String x) {
                int pos = x.indexOf('@');
                if (pos > 0) {
                        return x.substring(pos+1);
                } else {
                        return null;
                }
        }

	private String domain(String[] x) {
                return domain(x[0]);
	}
	
	private boolean isAffiliation(String[] affiliations, String a) {
		for (String affiliation : affiliations) {
			if (affiliation.contains("@")) {
				String parts[] = affiliation.split("@");
				if (parts[0].equalsIgnoreCase(a))
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

		final HttpServletRequest req = (HttpServletRequest) request;
		final HttpServletResponse resp = (HttpServletResponse) response;
		final String uid = req.getHeader("X_REMOTE_USER");
		System.err.println("request uri=" + req.getRequestURI());
		System.err.println("uid=" + uid);
		if (req.getRequestURI().contains("/system/login")
				&& !isNullOrEmpty(uid)) {
			ACPClient client = null;
			try {
				System.err.println("in doFilter");

				if (!uid.contains("@")) {
					resp.sendRedirect("https://"+request.getServerName()+"/errors/missing-attribute.php?attribute=eduPersonPrincipalName");
					return;
				}

				if (!hasProperty(req, "first-name")) {
					resp.sendRedirect("https://"+request.getServerName()+"/errors/missing-attribute.php?attribute=givenName");
					return;
				}

				if (!hasProperty(req, "last-name")) {
					resp.sendRedirect("https://"+request.getServerName()+"/errors/missing-attribute.php?attribute=sn");
					return;
				}

				client = ACPClient.open(apiUrl, user, password, httpClient, pCache);
				
				ACPPrincipal user = client.findOrCreatePrincipal("login", uid,
						"user", new HashMap<String, String>() {
							/**
							 * 
							 */
							private static final long serialVersionUID = 1L;

							{
								put("type", "user");
								put("has-children", "0");
								put("first-name",
										getProperty(req, "first-name"));
								put("last-name", getProperty(req, "last-name"));
								put("email", getProperty(req, "email"));
								put("login", uid);
								put("ext-login", uid);
							}
						});

                                final String eppn_domain = domain(uid);
				ACPPrincipal domain_group = client.findOrCreatePrincipal("name",eppn_domain,
                                             "group", new HashMap<String, String>() {
                                                          /**
                                                          *
                                                          */
                                                          private static final long serialVersionUID = 1L;
                                                          {
                                                              put("type", "group");
                                                              put("has-children", "1");
                                                              put("name",eppn_domain);
                                                          }
                                             });
                                client.addMember(user.getPrincipalId(), domain_group.getPrincipalId());

				String unscopedAffiliations = getProperty(req,"unscoped_affiliation");
				if (!isNullOrEmpty(unscopedAffiliations)) {
					String [] a = unscopedAffiliations.split(";");
					for (final String affiliation : new String[] { "student" , "employee", "member", "affiliate", "alumn", "staff", "faculty" }) {
						ACPPrincipal group = client.findOrCreatePrincipal("name",affiliation,
								"group", new HashMap<String, String>() {
									/**
									 * 
									 */
									private static final long serialVersionUID = 1L;

									{
										put("type", "group");
										put("has-children", "1");
										put("name",affiliation);
									}
								});
						if (isPresent(a, affiliation)) {
							client.addMember(user.getPrincipalId(), group.getPrincipalId());
						} else {
							client.removeMember(user.getPrincipalId(), group.getPrincipalId());
						}

                                                if (eppn_domain != null) {
						   final String fake_scope_affiliation = affiliation + "@" + eppn_domain;
                                                   ACPPrincipal group_scoped = client.findOrCreatePrincipal("name",fake_scope_affiliation,
                                                                "group", new HashMap<String, String>() {
                                                                        /**
                                                                         *
                                                                         */
                                                                        private static final long serialVersionUID = 1L;

                                                                        {
                                                                                put("type", "group");
                                                                                put("has-children", "1");
                                                                                put("name",fake_scope_affiliation);
                                                                        }
                                                                });
                                                   if (isPresent(a, affiliation)) {
                                                        client.addMember(user.getPrincipalId(), group_scoped.getPrincipalId());
                                                   } else {
                                                        client.removeMember(user.getPrincipalId(), group_scoped.getPrincipalId());
                                                   }
                                                }
					}
				}
				
				System.err.println("principal=" + user.getPrincipalId());
				String affiliations = getProperty(req, "affiliation");
				System.err.println("affiliation=" + affiliations);

				if (!isNullOrEmpty(affiliations)) {
					String[] a = affiliations.split(";");
					if (isMemberOrEmployee(a)) {
						ACPPrincipal liveAdmins = client.findBuiltIn("live-admins");
						if (liveAdmins != null)
							client.addMember(user.getPrincipalId(), liveAdmins.getPrincipalId());
						//ACPPrincipal seminarAdmins = client.findBuiltIn("seminar-admins");
						//if (seminarAdmins != null)
					        //		client.addMember(user.getPrincipalId(),seminarAdmins.getPrincipalId());
					} else {
						ACPPrincipal liveAdmins = client.findBuiltIn("live-admins");
						if (liveAdmins != null)
							client.removeMember(user.getPrincipalId(),liveAdmins.getPrincipalId());
						//ACPPrincipal seminarAdmins = client.findBuiltIn("seminar-admins");
						//if (seminarAdmins != null)
					        //		client.removeMember(user.getPrincipalId(),seminarAdmins.getPrincipalId());
					}
					
					String domain = domain(a);
					for (String affiliation : new String[] { "student" , "employee", "member", "affiliate", "alumn", "staff", "faculty" }) {
						final String name = affiliation+"@"+domain;
						ACPPrincipal group = client.findOrCreatePrincipal("name",name,
								"group", new HashMap<String, String>() {
									/**
									 * 
									 */
									private static final long serialVersionUID = 1L;

									{
										put("type", "group");
										put("has-children", "1");
										put("name",name);
									}
								});
						if (isAffiliation(a, affiliation)) {
							client.addMember(user.getPrincipalId(), group.getPrincipalId());
						} else {
							client.removeMember(user.getPrincipalId(), group.getPrincipalId());
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new ServletException(ex);
			}
		}

		chain.doFilter(request, response);
	}

	private boolean isPresent(String[] a, String affiliation) {
		for (final String af : a) {
			if (af.equalsIgnoreCase(affiliation))
				return true;
		}
		return false;
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

	
	private String apiUrl;
	private String user;
	private String password;
	private ACPHTTPClient httpClient;
        private Cache<ACPPrincipal> pCache;
	
	@SuppressWarnings("unchecked")
	public void init(FilterConfig filterConfig) throws ServletException {
		try {
                        int cacheTtl = DEFAULT_CACHE_TTL;
                        String cacheTtlStr = filterConfig.getInitParameter("acp.cache-ttl");
                        if (cacheTtlStr != null) {
                         	try {
                             		Integer t = Integer.parseInt(cacheTtlStr);
      					cacheTtl = t.intValue();
                                } catch (NumberFormatException ex) {
					ex.printStackTrace();
                                }
                        }
                        pCache = new Cache<ACPPrincipal>(cacheTtl);
			apiUrl = filterConfig.getInitParameter("acp.url");
			user = filterConfig.getInitParameter("acp.user");
			password = filterConfig.getInitParameter("acp.password");
			String clientClassName = filterConfig
					.getInitParameter("acp.http-client");
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

			if (clientClassName != null) {
				Class<ACPHTTPClient> clientClass = (Class<ACPHTTPClient>) Class
						.forName(clientClassName);
				httpClient = clientClass.newInstance();
			}
			headerMap = new HashMap<String, List<String>>();
			headerMap.put("first-name", parseHeaderConfig(filterConfig,
					"acp.header-map.first-name"));
			headerMap.put("last-name", parseHeaderConfig(filterConfig,
					"acp.header-map.last-name"));
			headerMap.put("email", parseHeaderConfig(filterConfig,
					"acp.header-map.email"));
		} catch (Exception ex) {
			throw new ServletException(ex);
		}
	}

}
