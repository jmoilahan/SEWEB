package fi.seweb.client.util;

public enum ValidityInterval {
	
	IMMEDIATE(0),
	SHORT(15),
	MEDIUM(60),
	LONG(300);
	
	private final int validity; //in seconds;
	
	ValidityInterval(int val) {
		this.validity = val;
	}
	
	public int validity() { return validity; }
	
}
