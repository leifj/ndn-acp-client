package net.nordu.acp.client.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

import net.nordu.acp.client.ACPClient;
import net.nordu.acp.filters.UserProvisionFilter;

import org.junit.Test;
import org.mortbay.jetty.Server;

public class UserProvisionFilterTest {
	
	private class PropertiesFilterConfig implements FilterConfig {
		
		private Properties p;
		
		public PropertiesFilterConfig() throws IOException {
			this(null);
		}
		
		public PropertiesFilterConfig(String source) throws IOException {
			if (source == null) {
				p = System.getProperties();
			} else {
				p = new Properties();
				p.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(source));
			}
		}
		
		public String getFilterName() {
			return "dummy";
		}

		public String getInitParameter(String arg0) {
			return p.getProperty(arg0);
		}

		@SuppressWarnings("unchecked")
		public Enumeration getInitParameterNames() {
			return p.propertyNames();
		}

		public ServletContext getServletContext() {
			throw new IllegalArgumentException("Why would you want a ServletContext?");
		}
	}

	@Test
	public void testInitFilter() {
		UserProvisionFilter filter = new UserProvisionFilter();
		try {
			filter.init(new PropertiesFilterConfig("filter-config-1.properties"));
			Map<String, List<String>> hm = filter.getHeaderMap();
			assertNotNull(hm);
			
			assertNull(hm.get("not-there"));
			for (String av : new String[] {"first-name:givenName","last-name:sn","email:mail"}) {
				int ci = av.indexOf(':');
				String a = av.substring(0,ci);
				String v = av.substring(ci+1);
				assertNotNull(hm.get(a));
				assertTrue(hm.get(a).contains(v));
				assertFalse(hm.get("first-name").contains("annat"));
			}
			assertNotNull(filter.getClientPool());
			Object client = filter.getClientPool().borrowObject();
			assertNotNull(client);
			assertTrue(client instanceof ACPClient);
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}
	
	@Test
	public void testDoFilter() {
		UserProvisionFilter filter = new UserProvisionFilter();
		try {
			if (System.getProperty("acp.url") != null) //use sysenv
				filter.init(new PropertiesFilterConfig());
			else
				filter.init(new PropertiesFilterConfig("filter-config-1.properties"));
			
			assertNotNull(filter.getHeaderMap());
			
			
		} catch (Exception ex) {
			ex.printStackTrace();
			fail(ex.getMessage());
		}
	}

}
