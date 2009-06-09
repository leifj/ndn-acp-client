package net.nordu.acp.client;

public class ACPException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -492010406703352061L;

	public ACPException() {
		super();
	}
	
	public ACPException(Object message) {
		super(message.toString());
	}
	
	public ACPException(Exception inner) {
		super(inner);
	}
	
}
