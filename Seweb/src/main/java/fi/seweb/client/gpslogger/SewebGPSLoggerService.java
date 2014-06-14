package fi.seweb.client.gpslogger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;
import fi.seweb.client.app.UIThread;
import fi.seweb.client.common.SewebPreferences;
import fi.seweb.client.core.RosterCache;
import fi.seweb.client.db.RosterTable;

public class SewebGPSLoggerService extends Service {
	private static final String TAG = "SewebGPSLoggerService";

	private RosterCache mRosterDatabase;
	private SewebPreferences mConfig;
	private String mServletURL;
	private String mJid;
	private Timer mTimer = new Timer();
	private LocationManager mLocationManager;
    private LocationListener mLocationListener;
    
    private static final long minTimeMillis = 2000;
    private static final long minDistanceMeters = 10;
    private static final float minAccuracyMeters = 35;
    
    private final DecimalFormat sevenSigDigits = new DecimalFormat("0.#######");
        
    private int mLastStatus = LocationProvider.OUT_OF_SERVICE;
    private String mLastLongitude = null;
    private String mLastLatitude = null;
    private long mLastRecordedTimestamp = 0; 
		
	@Override
	public void onCreate() {
		Log.i(TAG, "onCreate() called");
		super.onCreate();
				
		mConfig = new SewebPreferences(PreferenceManager
				.getDefaultSharedPreferences(this)); 
		
		//http://188.226.173.230:8080/SewebServer/Seweb
		mServletURL = mConfig.SERVLET_URL;
		mJid = mConfig.getMyFullJid();
		
		mRosterDatabase = new RosterCache(getApplicationContext());
		
		//getApplicationContext();
		// ---use the LocationManager class to obtain GPS locations---
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	}
	
	@Override 
	public int onStartCommand(final Intent intent, final int flags, final int startId) {
		Log.i(TAG, "onStartCommand() called");
		Toast.makeText(this, TAG + " started", Toast.LENGTH_SHORT).show();
		
        mLocationListener = new SewebLocationListener();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        minTimeMillis,
                        minDistanceMeters,
                        mLocationListener);
		
		mTimer.schedule(new TimerTask() {
	        // just call the handler every 10 Seconds
			@Override public void run() {
		 		String output = null;
		        try {
		        	if (mLastLatitude != null && mLastLongitude != null) {
		        		DateTime timeNow = DateTime.now();
		        		DateTime timestamp = new DateTime(mLastRecordedTimestamp);
		        		Period p = new Period(timestamp, timeNow);
		        		
		        		if (p.getDays() == 0 && p.getHours() == 0 && p.getMinutes() <= 3) {
		        			//String URL = "http://188.226.173.230:8080/SewebServer/Seweb?jid=oleg&location=33.127143&location=-127.1238923";
		        			String URL = mServletURL + "?jid=" + mJid + "&location=" + mLastLatitude + "&location=" + mLastLongitude; 
		        	
		        			DefaultHttpClient httpClient = new DefaultHttpClient();
		        			HttpGet httpGet = new HttpGet(URL);
			 
		        			HttpResponse httpResponse = httpClient.execute(httpGet);
		        			HttpEntity httpEntity = httpResponse.getEntity();
		        			output = EntityUtils.toString(httpEntity);
		        		}
		        	}
		        	
		        	mRosterDatabase.update();
		        	
		        } catch (UnsupportedEncodingException e) {
		            e.printStackTrace();
		        } catch (ClientProtocolException e) {
		            e.printStackTrace();
		        } catch (IOException e) {
		            e.printStackTrace();
		        }
		            
		        if (output != null && output.length() != 0) {
		           	parseRequestResult(output);
		           	Log.i(TAG, "Fetched from the server... " + output);
		        }
		    }       
		}, 1000,10000); 
		return Service.START_STICKY;
	}
	
	private void parseRequestResult(String in) {
		
		//String test = "{\"distances\":[]}";
		
		//HashMap<String, Double> distances = new HashMap<String, Double>();
		HashMap<String, Distance> distances = new HashMap<String, Distance>();
		try {
			JSONObject all = new JSONObject(in);
			JSONArray array = all.getJSONArray("distances");
		
			for (int i=0; i < array.length(); i++) {
				JSONObject object = (JSONObject) array.get(i);
				String jid = (String) object.get("jid");
				if (!jid.equalsIgnoreCase(mJid)) {
					Double distance = object.getDouble("distance");
					long timestamp = object.getLong("timestamp");
					Distance d = new Distance(jid, timestamp, distance);
					//distances.put(jid, distance);
					distances.put(jid, d);
				}
			}
			
			if (!distances.isEmpty()) {
				//Iterator<Entry<String, Double>> i = distances.entrySet().iterator();
				Iterator<Entry<String, Distance>> i = distances.entrySet().iterator();
				StringBuilder builder = new StringBuilder();
				builder.append("Distances");
				while (i.hasNext()) {
					Entry<String, Distance> e = i.next();
					String jid = (String) e.getKey();
					String name = StringUtils.parseBareAddress(jid);
					//Double distance = (Double) e.getValue();
					Distance d = (Distance) e.getValue();
					double distance = d.getDistance();
					mRosterDatabase.setDistance(jid, (int) distance, d.getTimestamp());
					
					int days = d.getDays();
					int hours = d.getHours();
					int minutes = d.getMinutes();
					int seconds = d.getSeconds();
					
					builder.append("\n" + name + " was: ");
					builder.append(distance + "m ");
					builder.append("\n" + hours + ":" + minutes + ":" + seconds + " ago");
				}
				Log.i(TAG, builder.toString());
				//UIThread.toast(this, builder.toString(), Toast.LENGTH_LONG);
			}
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage());
		} catch (ClassCastException e) {
			Log.e(TAG, e.getMessage());
		}
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy() called");
		super.onDestroy();
		// cleanup: kill threads, registered listeners, receivers
		
		mLocationManager.removeUpdates(mLocationListener);

		// Tell the user we stopped.
        Toast.makeText(this, TAG + " stopped", Toast.LENGTH_SHORT).show();
	}
	
	private final Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override public void handleMessage(android.os.Message msg) {
			Log.i(TAG, "handleMessage() called");
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public class SewebLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
        	Log.i(TAG, "onLocationChanged()");
        	if (loc != null) {
        		boolean pointIsRecorded = false;
                	try {
                		if (loc.hasAccuracy() && loc.getAccuracy() <= minAccuracyMeters) {
                			pointIsRecorded = true;
                			
                			Double latitude = loc.getLatitude();
                			Double longitude = loc.getLongitude();
                			mLastLatitude = sevenSigDigits.format(latitude);
                			mLastLongitude = sevenSigDigits.format(longitude);
                			mLastRecordedTimestamp = DateTime.now().getMillis();
                			
                			/*
                			GregorianCalendar greg = new GregorianCalendar();
                			TimeZone tz = greg.getTimeZone();
                			int offset = tz.getOffset(System.currentTimeMillis());
                			greg.add(Calendar.SECOND, (offset/1000) * -1);
                			*/
                			
                			/*StringBuffer queryBuf = new StringBuffer();
                			queryBuf.append("INSERT INTO "+POINTS_TABLE_NAME+
                        		  " (GMTTIMESTAMP,LATITUDE,LONGITUDE,ALTITUDE,ACCURACY,SPEED,BEARING) VALUES (" +
                        		  "'"+timestampFormat.format(greg.getTime())+"',"+
                                  loc.getLatitude()+","+
                                  loc.getLongitude()+","+
                                  (loc.hasAltitude() ? loc.getAltitude() : "NULL")+","+
                                  (loc.hasAccuracy() ? loc.getAccuracy() : "NULL")+","+
                                  (loc.hasSpeed() ? loc.getSpeed() : "NULL")+","+
                                  (loc.hasBearing() ? loc.getBearing() : "NULL")+");");
                			Log.i(tag, queryBuf.toString());
                			db = openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.OPEN_READWRITE, null);
                			db.execSQL(queryBuf.toString());*/
                       }
                	} catch (Exception e) {
                		Log.e(TAG, e.toString());
                    } finally {
                    	/*if (db.isOpen())
                    		db.close();*/
                    }
                    
                	if (pointIsRecorded) {
                		Toast.makeText(
                		getBaseContext(),
                        /*"Location recorded: \nLat: " + sevenSigDigits.format(loc.getLatitude())*/
                        	/*+ " \nLon: " + sevenSigDigits.format(loc.getLongitude())*/ 
                        	/*+ " \nAlt: " + (loc.hasAltitude() ? loc.getAltitude()+"m":"?") */
                			"Location recorded: "
                        	 + " Acc: " + (loc.hasAccuracy() ? loc.getAccuracy()+"m":"?"),
                        Toast.LENGTH_LONG).show();
                	
                    } else {
                    	Toast.makeText(
                        getBaseContext(),
                        "Location not accurate enough",
                        Toast.LENGTH_SHORT).show();
                    }
            }
        }
        
        @Override
        public void onProviderEnabled(String provider) {
        	Log.i(TAG, "onProviderEnabled()");
            Toast.makeText(getBaseContext(), "onProviderEnabled: " + provider,
            Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        	String showStatus = null;
            if (status == LocationProvider.AVAILABLE)
                showStatus = "Available";
            if (status == LocationProvider.TEMPORARILY_UNAVAILABLE)
                showStatus = "Temporarily Unavailable";
            if (status == LocationProvider.OUT_OF_SERVICE)
            	showStatus = "Out of Service";
            if (status != mLastStatus) {
              	Toast.makeText(getBaseContext(),
              		"GPS status: " + showStatus,
               		Toast.LENGTH_SHORT).show();
            }
            mLastStatus = status;
            Log.i(TAG, "GPS status: " + showStatus);
        }

		@Override
		public void onProviderDisabled(String provider) {
			Log.i(TAG, "onProviderDisabled()");
			Toast.makeText(getBaseContext(), "onProviderDisabled: " + provider,
	                Toast.LENGTH_SHORT).show();			
		}
	}
    
	private class Distance {
		final long timestamp;
		final double distance;
		final String jid;
		final DateTime timeOld;
		
		public Distance(String jid, long timestamp, double distance) {
			this.jid = jid;
			this.distance = distance;
			timeOld = new DateTime(timestamp);
			this.timestamp = timestamp;
		}
		
		public long getTimestamp() {
			return timestamp;
		}
		
		public int getDays () {
			DateTime timeNow = DateTime.now();
			Period period = new Period(timeOld, timeNow);
			return period.getDays();
		}
		
		public int getHours() {
			DateTime timeNow = DateTime.now();
			Period period = new Period(timeOld, timeNow);
			return period.getHours();
		}
		
		public int getMinutes() {
			DateTime timeNow = DateTime.now();
			Period period = new Period(timeOld, timeNow);
			return period.getMinutes();
		}
		
		public int getSeconds() {
			DateTime timeNow = DateTime.now();
			Period period = new Period(timeOld, timeNow);
			return period.getSeconds();
		}
		
		public double getDistance() {
			return distance;
		}
		
		public String getJid() {
			return jid;
		}
	}
}

