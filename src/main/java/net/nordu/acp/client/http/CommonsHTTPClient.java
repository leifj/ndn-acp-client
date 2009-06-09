package net.nordu.acp.client.http;

import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;

import net.nordu.acp.client.ACPException;

public class CommonsHTTPClient implements ACPHTTPClient {

	private HttpClient http;
	
	public CommonsHTTPClient() {
		http = new HttpClient();
	}
	
	public InputStream GET(String url, String cookie) throws ACPException {
		try {
			HttpMethod method = new GetMethod(url);
			int code = http.executeMethod(method);
			if (code != 200)
				throw new ACPException(method.getStatusLine().toString());
			
			return method.getResponseBodyAsStream();
		} catch (Exception ex) {
			throw new ACPException(ex);
		}
	}

}
