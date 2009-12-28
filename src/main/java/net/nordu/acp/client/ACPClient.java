package net.nordu.acp.client;

import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.nordu.acp.client.http.ACPHTTPClient;
import net.nordu.acp.client.http.SimpleHTTPClient;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ACPClient {

	private Log log = LogFactory.getLog(ACPClient.class);
	
	private String apiUrl;
	private String session;
	private HttpClient http;
	private long createTime;
	
	protected ACPClient(String apiUrl, ACPHTTPClient httpClient) {
		this.apiUrl = apiUrl;
		this.http = new HttpClient();
		this.createTime = (new Date()).getTime();
	}
	
	protected ACPClient(String apiUrl) throws ACPException {
		this(apiUrl,new SimpleHTTPClient());
	}
	
	public String getSession() {
		return session;
	}
	
	public void setSession(String session) {
		this.session = session;
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
	
	private Element makeParam(Document doc, String name, String value) {
		Element param = doc.createElement("param");
		param.setAttribute("name", name);
		Node textNode = doc.createTextNode(value);
		param.appendChild(textNode);
		return param;
	}
	
	public ACPResult request_post(String action, Map<String, String> p) throws ACPException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.newDocument();
			Node paramsNode = doc.appendChild(doc.createElement("params"));
			paramsNode.appendChild(makeParam(doc, "action", action));
			if (getCookie() != null)
				paramsNode.appendChild(makeParam(doc, "session", getCookie()));
			if (p != null && p.size() > 0) {
				for (Map.Entry<String, String> e : p.entrySet()) {
					paramsNode.appendChild(makeParam(doc, e.getKey(), e.getValue()));
				}
			}
			
			//set up a transformer
            TransformerFactory transfac = TransformerFactory.newInstance();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            
            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            DOMSource source = new DOMSource(doc);
            trans.transform(source, result);
            
            PostMethod post = new PostMethod(apiUrl);
			post.addRequestHeader("Content-Type","text/xml");
			post.setRequestBody(sw.toString());
			int code = http.executeMethod(post);
			if (code != 200)
				throw new ACPException(post.getStatusLine().toString());
			
			Header cookieHeader = post.getResponseHeader("Set-Cookie");
			if (cookieHeader != null && cookieHeader.getValue() != null) {
				String ch = cookieHeader.getValue();
				int sp = ch.indexOf(";");
				int ep = ch.indexOf("=");
				String s = ch.substring(ep+1, sp);
				System.err.println("Found session: "+s);
				setSession(s);
			}
            
			ACPResult r = ACPResult.parse(post.getResponseBodyAsStream());
			log.info(r);
			System.err.println(r);
			return r;
		} catch (Exception ex) {
			throw new ACPException(ex);
		}
	}
	
	public ACPResult request(String action, Map<String, String> p) throws ACPException {
		try {
			
			StringBuffer url = new StringBuffer();
			url.append(apiUrl).append("?").append("action=").append(action);
			if (isInSession()) {
				url.append("&session="+getSession());
			}
			if (p != null && p.size() > 0) {
				for (Map.Entry<String, String> e : p.entrySet()) {
                    if (e.getValue() != null && e.getValue().length() > 0) {
					   url.append("&");
					   url.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(),"ISO-8859-1"));
                    }
				}
			}
			System.err.println("http="+http);
			System.err.println("url="+url);
			
			HttpMethod get = new GetMethod(url.toString());
			int code = http.executeMethod(get);
			if (code != 200)
				throw new ACPException(get.getStatusLine().toString());
			
			Header cookieHeader = get.getResponseHeader("Set-Cookie");
			if (cookieHeader != null && cookieHeader.getValue() != null) {
				String ch = cookieHeader.getValue();
				int sp = ch.indexOf(";");
				int ep = ch.indexOf("=");
				String s = ch.substring(ep+1, sp);
				System.err.println("Found session: "+s);
				setSession(s);
			}
			
			ACPResult r = ACPResult.parse(get.getResponseBodyAsStream());
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
		/*
		for (Map.Entry<String, String> e : properties.entrySet()) {
			if (e.getValue() == null || e.getValue().length() == 0)
				properties.remove(e.getKey());
		}
		*/
		
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
	
	public ACPPrincipal findGroup(String name) throws ACPException {
		Map<String, String> request = new HashMap<String, String>();
		request.put("filter-name", name);
		request.put("filter-type", "group");
		ACPResult groupResult = request("principal-list", request);
		ACPPrincipal principal = groupResult.getPrincipal();
		if (groupResult.isError())
			throw groupResult.getException();
		
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
