package fi.seweb.client.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;


import android.util.Log;


public class Event implements Serializable {
	
	
	private static final long serialVersionUID = -5063767779772842961L;
	
	//these values are set by the inner builder class 
	private transient String payload;
	private transient ValidityInterval validity; //default is SHORT = 1 minute.
	private transient Channel channel;
	
	// this value is computed at the object creation time 
	// & cached for performance purposes
	private byte[] byteArray;
	
	public static class Builder {
		//required parameters
		private transient final String payload;
		private transient final Channel channel;
		
		//optional parameters
		private transient ValidityInterval validity = ValidityInterval.SHORT;
		
		/*
		 *  @param payload the content of the event, must be non null, payload.length must be greater than 0 
		 *  @param channel the destination channel of the event, must be non null
		 *  @throws IllegalArgumentException if either parameter is null
		 *  @throws IllegalArgumentException if payload.length equals zero
		 */
		public Builder(String payload, Channel channel) {
			if (payload == null)
				throw new IllegalArgumentException("Payload parameter is null");
			if (channel == null)
				throw new IllegalArgumentException("Channel parameter is null");
			if (payload.length() == 0)
				throw new IllegalArgumentException("Payload parameter (string) is empty");
			
			this.payload = payload;
			this.channel = channel;
		}
		
		
		public Builder(byte[] data) {
			if (data == null)
				throw new IllegalArgumentException("data parameter is null");
			
			//Temporary payload field.
			String temp = null;
			Channel channelTmp = null;
			

			// now we need to extract payload and validity interval from data
			ByteArrayInputStream bos = new ByteArrayInputStream(data);
			ObjectInput out = null;
			
			try {
				out = new ObjectInputStream(bos);
				Object obj;			
				Event e;
				
				obj = out.readObject();
				
				if (obj == null)
					throw new NullPointerException("Failed to read objects from byte array");
				
				// what if the serialized object was an event?
				if (obj instanceof Event) {
					e = (Event) obj;
					temp = e.getPayload();
					validity = e.getValidity();
					channelTmp = e.getChannel();
					
				} else if (obj instanceof String) {
					
					temp = (String) obj;
					
					obj = out.readObject();
					if ((obj != null) && (obj instanceof ValidityInterval))
						validity = (ValidityInterval) obj;
					
					obj = out.readObject();
					if ((obj != null) && (obj instanceof Channel))
						channelTmp = (Channel) obj;
					
				}
		
				out.close();
				bos.close();
				
			} catch (Exception e) {
				throw new AssertionError(e.getMessage());
			}
			
			if (temp == null)
				throw new IllegalStateException("Couldn't initialize the payload field, it is null");
			if (temp.length() == 0)
				throw new IllegalStateException("Couldn't initialize the payload field, its length = 0");
			if (channelTmp == null)
				throw new IllegalStateException("Couldn't initialize the channel field, it is null");
			
			this.payload = temp;
			this.channel = channelTmp;
		}
		
		public Builder validity (ValidityInterval val) {
			validity = val;
			return this;
		}
		
		// below 
		// implement the other methods that set optional parameters;
		
		public Event build() {
			return new Event(this);
		}
	}
	
	private Event(Builder builder) {
		payload = builder.payload;
		channel = builder.channel;
		validity = builder.validity;
		
		byteArray = computeByteArray(payload, validity, channel);
	}
	
	private byte[] computeByteArray(String payload, ValidityInterval validity, Channel channel) {
		byte[] result = null;
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutput out = null;
		
		try {
			out = new ObjectOutputStream(bos);   
			out.writeObject(payload);
			out.writeObject(validity);
			out.writeObject(channel);
		
			result = bos.toByteArray();
		
			out.close();
			bos.close();
		} catch (Exception ex) {
			Log.e("[Event Dispatcher]", "Failed to compute a byte array.");
			throw new IllegalStateException("Error while computing the internal byte array");
		}
		
		Log.i("[Event Dispatcher]", "Serialization copy is created");
		
		return result; 
	}
	
	public byte[] asByteArray() {
		return byteArray;
	}
	
	public String getPayload() {
		return payload;
	}
	
	public ValidityInterval getValidity() {
		return validity;
	}

	public Channel getChannel() {
		return channel;
	}
	
	// serialization machinery
	
	private void readObject(ObjectInputStream s)
		throws IOException, ClassNotFoundException {
		
		s.defaultReadObject();
		
		ValidityInterval val = null;
		Channel channelTmp = null;
		
		String content = s.readUTF();
								
		if ( (content != null) && (content.length() != 0) ) {
			this.payload = content;
		} else {
			throw new IllegalArgumentException("Invalid payload argument (null or length() equals zero)");
		}
		
		Object obj = s.readObject(); // validity
		
		if (obj != null) {
			if (obj instanceof ValidityInterval) {
				val = (ValidityInterval) obj;
			} else
				throw new IllegalArgumentException("Invalid validity interval: should be an instance of ValidityInterval class");
		} else {
			throw new IllegalArgumentException("Invalid validity interval argument (null)");
		}
		
		obj = s.readObject(); // channel
		if (obj != null) {
			if (obj instanceof Channel) {
				channelTmp = (Channel) obj;
			} else
				throw new IllegalArgumentException("Invalid channel: should be an instance of Channel class");
		} else {
			throw new IllegalArgumentException("Invalid channel argument (null)");
		}
		
		
		this.channel = channelTmp;
		this.validity = val;
		
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		
		out.writeUTF(this.payload);
		out.writeObject(this.validity);
		out.writeObject(this.channel);
	}
	
}
	


