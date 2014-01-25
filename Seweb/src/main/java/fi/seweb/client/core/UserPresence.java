package fi.seweb.client.core;

import fi.seweb.client.common.StringUtils;

public class UserPresence {
	public final String user;
	public final int presenceCode;
	public final String statusMessage;
	public final boolean unreadMessages;
	
	private UserPresence (UserPresence.Builder builder) {
		this.user = builder.user;
		this.presenceCode = builder.code;
		this.statusMessage = builder.message;
		this.unreadMessages = builder.hasMessages;
	}
	
	public boolean hasUnreadMessages() {
		return unreadMessages;
	}
	
	/* ------------------------------------------------- */
	
	public static class Builder {
		// required parameters
		private final String user;
		private final int code;
		private final String message;
		
		// optional parameter (has a default value)
		boolean hasMessages = false;
	
		public Builder (String user, int presenceCode, String statusMessage) {
			
			if (user == null || user.length() == 0)
				throw new IllegalArgumentException("User jid param is empty / null");
			if (statusMessage == null)
				throw new IllegalArgumentException("Status message param is null");
			if (presenceCode < 0)
				throw new IllegalArgumentException("Presence code param has a negative value: " + presenceCode);
			if (!StringUtils.isValidJid(user))
				throw new IllegalArgumentException("Invalid jid param: " + user);
			
			this.user = user;
			this.code = presenceCode;
			this.message = statusMessage;
		}
		
		public Builder setUnreadMessage() {
			this.hasMessages = true;
			return this;
		}
		
		public UserPresence build() {
			return new UserPresence(this);
		}
	}
}
