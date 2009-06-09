package net.nordu.acp.client;

public class ACPAuthenticationException extends ACPException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5859372713525472260L;

	public ACPAuthenticationException() {
		super();
	}
	
	public ACPAuthenticationException(Object message) {
		super(message);
	}
	
	public ACPAuthenticationException(Exception inner) {
		super(inner);
	}
	
}
