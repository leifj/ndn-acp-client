package net.nordu.acp.client.http;

import java.io.InputStream;

import net.nordu.acp.client.ACPException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class CommonsHTTPClient implements ACPHTTPClient {

	private HttpClient http;
	
	public CommonsHTTPClient() {
		http = new HttpClient();
		http.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
	}
	
	public InputStream GET(String url, String cookie) throws ACPException {
		try {
			HttpMethod get = new GetMethod(url);
			int code = http.executeMethod(get);
			if (code != 200)
				throw new ACPException(get.getStatusLine().toString());
			
			
			
			return get.getResponseBodyAsStream();
		} catch (Exception ex) {
			throw new ACPException(ex);
		}
	}

	public InputStream POST(String url, String content)
			throws ACPException {
		try {
			PostMethod post = new PostMethod(url);
			post.addRequestHeader("Content-Type","text/xml");
			post.setRequestBody(content);
			int code = http.executeMethod(post);
			if (code != 200)
				throw new ACPException(post.getStatusLine().toString());
			
			return post.getResponseBodyAsStream();
		} catch (Exception ex) {
			throw new ACPException(ex);
		}
	}

}
