package fi.seweb.server.dao;

import java.text.SimpleDateFormat;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

public class Position {
	private final int id;
	private final double latitude;
	private final double longitude;
	private final long timestamp; //in milliseconds!
	private final String jid;
	
	// Private constants:
	private final static String DATE = "HH:mm:ss";
	private final static SimpleDateFormat FORMAT = new SimpleDateFormat(DATE);
	
	public Position(int id, double latitude, double longitude, long timestamp, String jid) {
		this.id = id;
		this.latitude = latitude;
		this.longitude = longitude;
		this.timestamp = timestamp;
		this.jid = jid;
	}
	
	@Override
    public String toString() {
        return String.format(
                "Position [id=%d, lat=%f, long=%f, time=%s, user=%s]",
                id, latitude, longitude, getElapsedTime(), jid);
    }
	
	public String getJid() {
		return jid;
	}
	
	public double getLatitude() {
		return latitude;
	}
	
	public double getLongitude() {
		return longitude;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public long getElapsedTimeMillis() {
		DateTime dt = new DateTime();
		long timeNow = dt.getMillis();
		return timeNow - timestamp; 
	}
	
	public String getElapsedTime() {
		DateTime timeNow = DateTime.now();
		DateTime timeOld = new DateTime(timestamp);
		
		Period period = new Period(timeOld, timeNow);
		PeriodFormatter HHMMSSFormater = new PeriodFormatterBuilder()
			.printZeroAlways()
			.minimumPrintedDigits(2)
			.appendMonths()
			.appendSeparator("d:")
			.appendDays()
			.appendSeparator("H:")
			.appendHours()
			.appendSeparator("m:")
			.appendMinutes()
			.appendSeparator("s:")
			.appendSeconds()
			.toFormatter(); // produce thread-safe formatter
		return HHMMSSFormater.print(period);
	}
	
}
