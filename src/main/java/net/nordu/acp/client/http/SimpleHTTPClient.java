package net.nordu.acp.client.http;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.nordu.acp.client.ACPException;

public class SimpleHTTPClient implements ACPHTTPClient {
	
	private static final Log log = LogFactory.getLog(SimpleHTTPClient.class);
	
	public SimpleHTTPClient() {
		
	}
	
	public InputStream GET(String url, String cookie) throws ACPException {
		try {
			URL requestUrl = new URL(url.toString());
			URLConnection c = requestUrl.openConnection();
			if (cookie != null)
				c.addRequestProperty("Cookie", cookie);
			c.connect();
			if (log.isDebugEnabled())
				log.debug(url+" ["+cookie+"]");
			return c.getInputStream();
		} catch (Exception ex) {
			throw new ACPException(ex);
		}
	}

}
