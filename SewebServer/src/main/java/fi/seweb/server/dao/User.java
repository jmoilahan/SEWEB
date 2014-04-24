package fi.seweb.server.dao;

public class User {
	private final int id;
	private final String jid;
	private final boolean online;
	
	public User(int id, String jid, boolean online) {
		this.id = id;
		this.jid = jid;
		this.online = online;
	}
	
	@Override
    public String toString() {
        return String.format(
                "User [id=%d, jid='%s', present='%s']",
                id, jid, online);
    }
	
	public String getJid() {
		return jid;
	}
	
	public boolean getStatus() {
		return online;
	}
	
	public Character getStatusAsString() {
		Character c;
		if (online) {
			c = 'T';
		} else {
			c = 'F';
		}
		return c;
	}
	
	public int getId() {
		return id;
	}
}
