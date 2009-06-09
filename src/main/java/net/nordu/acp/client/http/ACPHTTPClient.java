package net.nordu.acp.client.http;

import java.io.InputStream;

import net.nordu.acp.client.ACPException;

public interface ACPHTTPClient {
	
	public abstract InputStream GET(String url, String cookie) throws ACPException;
	
}
