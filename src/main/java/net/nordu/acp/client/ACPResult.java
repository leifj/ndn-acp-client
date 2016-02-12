package net.nordu.acp.client;

import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class ACPResult {

	private static final Log log = LogFactory.getLog(ACPResult.class);
	
	private Document document;
	private Element status;
	
	public Document getDocument() {
		return document;
	}
	
	protected ACPResult(Document doc) {
		this.document = doc;
		this.status = null;
	}
	
	public static ACPResult parse(InputStream in) throws ACPException {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(false);
			dbf.setValidating(false);
			DocumentBuilder db = dbf.newDocumentBuilder();
			db.setErrorHandler(new ErrorHandler() {
				public void warning(SAXParseException exception)
						throws SAXException {
					log.warn(exception);
				}
				
				public void error(SAXParseException exception)
						throws SAXException {
					log.error(exception);
				}
				
				public void fatalError(SAXParseException exception)
						throws SAXException {
					log.fatal(exception);	
				}
			});
			Document doc = db.parse(in);
			return new ACPResult(doc);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new ACPException(ex);
		}
	}
	
	public String toString() {
		try {
	        TransformerFactory tf = TransformerFactory.newInstance();
	        Transformer transformer = tf.newTransformer();
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	
	        //create string from xml tree
	        StringWriter sw = new StringWriter();
	        StreamResult result = new StreamResult(sw);
	        DOMSource source = new DOMSource(document);
	        transformer.transform(source, result);
	
			return sw.toString();
		} catch (Exception ex) {
			return "<missing-transformer/>";
		}
	}
	
	public String getStatusCode() throws ACPException {
		Element status = getStatusElement();
		if (status == null)
			throw new ACPException("Invalid status element");
		
		String code = status.getAttribute("code");
		if (code == null)
			throw new ACPException("Invalid status element");
		
		return code;
	}
	
	public boolean isError() {
		Element status = getStatusElement();
		if (status == null)
			return true;
		
		return status.getAttribute("code").equals("ok") == false;
	}

        public String getError() {
                return this.toString();
        }

	//TODO improve this message to something human-readable
	public String getError2() {
		Element status = getStatusElement();
		if (status == null)
			return "Missing status element";
		
		String code = status.getAttribute("code");
		NodeList elts = status.getElementsByTagName(code);
		Element invalid = (Element)elts.item(0);
		if (invalid == null)
			return "Unknown";
		
		NamedNodeMap attrs = invalid.getAttributes();
		StringBuffer buf = new StringBuffer();
		buf.append(code+": ");
		for (int i = 0; i < attrs.getLength(); i++) {
			Attr a = (Attr)attrs.item(i);
			buf.append(" ").append(a.getName()).append("=").append(a.getValue());
		}
		return buf.toString();
	}
	
	public ACPException getException() {
		return new ACPException(getError());
	}
	
	private Element getStatusElement() {
		if (status == null) {
			NodeList elts = document.getElementsByTagName("status");
			if (elts == null || elts.getLength() == 0)
				return null;
			status = (Element)elts.item(0);
		}
		
		return status;
	}
	
	private String getChildValue(Element e, String n, boolean must) throws ACPException {
		NodeList elts = document.getElementsByTagName(n);
		if ((elts == null || elts.getLength() == 0)) {
			if (must)
				throw new ACPException("No "+n+" element");
			else
				return null;
		}
		Node ce = elts.item(0);
		Node txtn = ce.getFirstChild();
		return txtn == null ? null : txtn.getNodeValue();
	}
	
	public ACPPrincipal getPrincipal() throws ACPException {
		ACPPrincipal p = new ACPPrincipal();
		
		NodeList elts = document.getElementsByTagName("principal");
		if (elts == null || elts.getLength() == 0)
			return null;
		
		Element principalElement = (Element)elts.item(0);
		if (principalElement != null) {
			p.setPrincipalId(principalElement.getAttribute("principal-id"));
			p.setAccountId(principalElement.getAttribute("account-id"));
			p.setType(principalElement.getAttribute("type"));
			p.setName(getChildValue(principalElement,"name",false));
			p.setLogin(getChildValue(principalElement, "login",true));
			p.setEmail(getChildValue(principalElement, "email",false));
			p.setDescription(getChildValue(principalElement, "description", false));
		}
		
		return p;
	}

}
