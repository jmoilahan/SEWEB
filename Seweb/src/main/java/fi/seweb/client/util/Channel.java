package fi.seweb.client.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Channel implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3280716828030052049L;
	private String topic;
	private transient static final Pattern ALPHANUMERIC = Pattern.compile("[A-Za-z0-9]+");
	
	public Channel(String topic) {
		validate(topic); 
		this.topic = topic;
	}
	
	public String getTopic() {
		return topic;
	}
	
	//check if the topic is not null
	//not empty
	//does not contain special symbols
	private void validate(String topic) {
		if (topic == null)
			throw new IllegalArgumentException(" Topic is null ");
		if (topic.length() == 0)
			throw new IllegalArgumentException(" Topic is empty ");
		
		Matcher m = ALPHANUMERIC.matcher(topic);
		if (!m.matches())
			throw new IllegalArgumentException(" Topic contains illegal characters ");
	}
	
	// serialization machinery
	
	private void readObject(ObjectInputStream s)
		throws IOException, ClassNotFoundException {
			
		s.defaultReadObject();
			
		String topicTmp = s.readUTF();
			
		if (topicTmp == null)
			throw new IllegalArgumentException(" Serialization failed: Topic is null ");
		if (topicTmp.length() == 0)
			throw new IllegalArgumentException(" Serialization failed: Topic is empty ");
			
		this.topic = topicTmp;
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		
		out.writeUTF(this.topic);
	}
		
}
