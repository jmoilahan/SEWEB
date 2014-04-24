package fi.seweb.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.postgresql.ds.PGPoolingDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import fi.seweb.server.dao.Position;
import fi.seweb.server.dao.User;

public class Application extends HttpServlet {
	
	private enum State { NEW, INITIALIZING, INITIALIZED };
	private final AtomicReference<State> init = new AtomicReference<State>(State.NEW);
	private JdbcTemplate template;
	private static Logger log = LogManager.getLogger(Application.class.getName());
	
	@Override
	public void init() {
		log.debug("Application.init() is called");
		
		if (!init.compareAndSet(State.NEW, State.INITIALIZING))
			throw new IllegalStateException("Already initialized");
		
		//loading the driver
		log.debug("Loading the postgresql driver");
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			log.debug("Failed to load the driver: " + e.getMessage());
		}
		
		//preparing the datasource
		log.debug("Preparing the postgresql datasource");
		
		PGPoolingDataSource source = new PGPoolingDataSource();
   	 	source.setDataSourceName("jdbc/postgres");
   	 	source.setServerName("localhost");
   	 	source.setDatabaseName("locations");
   	 	source.setUser("seweb");
   	 	source.setPassword("steN2haj");
   	 	source.setMaxConnections(10);
   	 	
   	 	//getting the connection
   	 	Connection conn = null;
   	 	try {
   	 		conn = source.getConnection();
   	 	} catch (SQLException e) {
   	 		log.debug("Failed to get a connection: " + e.getMessage());
   	 	} finally {
   	 		if (conn != null) {
   	 			try { conn.close(); } catch (SQLException e) {}
   	 		}
   	 	}
   	 	
   	 	template = new JdbcTemplate(source);
   	 	
   	 	boolean isClosed = false;
   	 	try {
   	 		isClosed = conn.isClosed();
   	 	} catch (SQLException e) {
   	 		log.error("Failed to test the connection {} ", e.getMessage());
   	 	}
   	 	
   	 	log.debug("Initialization complete");
   	 	init.set(State.INITIALIZED);
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		    throws IOException, ServletException
		    {
		log.debug("doGet() is called");
		checkInit(); 

		// checking if the param list is empty or has some stuff in it
	   	Enumeration<String> params = request.getParameterNames();
	   	
	   	if (!params.hasMoreElements()) {
	   		//empty
	   		log.debug("An empty param list detected: listing the database content...");
	   		response.setContentType("text/html");
	   		PrintWriter out = response.getWriter();
	   		out.println("<html>");
	   		out.println("<body>");
	   		out.println("<h1>Seweb Database</h1>");
	   		
	   		out.println("<p>Users Table</p>");
	   		List<User> users = getUsers();
	   		if (users.isEmpty()) {
	   			out.println("The users table is empty <br>");
	   		} else {
	   			for (User user : users) {
	   				out.println(user); 
	   				out.println("<br>");
	   			}
	   		}

	   		out.println("<p>(Refined) Position Table </p>");
	   		List<Position> positionsGroupped = getPositions();
	   		if (positionsGroupped.isEmpty()) {
	   			out.println("The result set is empty <br>");
	   		} else {
	   			for (Position position : positionsGroupped) {
	   				out.println(position);
	   				out.println("<br>");
	   			}
	   		}
	   		
	   		out.println("<p>Positions Table</p>");
	   		List<Position> positions = getAllPositions();
	   		if (positions.isEmpty()) {
	   			out.println("The positions table is empty <br>");
	   		} else {
	   			for (Position position : positions) {
	   				out.println(position);
	   				out.println("<br>");
	   			}
	   		}
	   		out.println("</body>");
	   		out.println("</html>");
	   		out.close();
	   	} else {
	   		log.debug("Param list contains values: will query the db and return distances");
	   		
	   		// parse params
	   		String jid = request.getParameter("jid");
	   		String[] location = request.getParameterValues("location");
	   		Double latitude = Double.parseDouble(location[0]);
	   		Double longitude = Double.parseDouble(location[1]);

	   		// validate params
		   	checkRequestParams(jid, location);
	   		
		   	//add new values to the database
	   		putPosition(jid, latitude, longitude);
	   		
	   		//TODO: calculate "freshness" / validity of data
	   		
	   		response.setContentType("text/html");
	   		PrintWriter out = response.getWriter();
	   		String json = getDistances(jid, new LatLng(latitude, longitude));

	   		out.println(json);
	   		out.close();
	   	}
    }
	
	private String getDistances(String jid, LatLng point) {
	   JSONObject obj = new JSONObject();
	   obj.put("distances", new JSONArray());
	   String jsonEmpty = obj.toString();
	   
	   List<Position> positions = getPositions();
	   
	   if (positions.isEmpty()) 
		   return jsonEmpty;
			   
	   if (positions.size() == 1) {
		   Position position = positions.get(0);
		   if (position.getJid().equalsIgnoreCase(jid)) {
			   return jsonEmpty;
		   }
	   }
		   
	   JSONArray mainArray = new JSONArray();
	   Iterator<Position> iterator = positions.iterator();
		   
	   while(iterator.hasNext()) {
		   Position position = iterator.next();
		   String screenname = position.getJid();
		   if (!screenname.equalsIgnoreCase(jid)) {
			   LatLng remotePoint = new LatLng(position.getLatitude(), position.getLongitude()); 
		   
			   double distance = LatLngTool.distance(remotePoint, point, LengthUnit.METER);
			   double roundedDistance = Math.round( distance * 100.0 ) / 100.0;
			   long timestamp = position.getTimestamp();

			   JSONObject json = new JSONObject();
			   json.put("jid", screenname);
			   json.put("distance", roundedDistance);
			   json.put("timestamp", timestamp);
			   mainArray.put(json);
		   }
	   }
		   
	   JSONObject distancesObj = new JSONObject();
	   distancesObj.put("distances", mainArray);
		   
	   return distancesObj.toString();
	}
	
	private void checkRequestParams(String jid, final String[] points) {
		log.debug("checkRequestParams() called");
		
		if (jid == null || jid.isEmpty()) {
			log.error("jid parameter is null or empty");
			throw new IllegalArgumentException("jid parameter is null or empty");
		}
		if (points == null) {
			log.error("location points parameter array is null");
			throw new IllegalArgumentException("location points parameter array is null");
		} 
		if (points.length != 2) {
			log.error("location parameter array should contain exactly 2 elements");
			log.error("got {} instead", points.length);
			throw new IllegalArgumentException("location parameter array should contain exactly 2 elements");
		}
		
		String latitude = points[0];
		String longitude = points[1];
		
		if (latitude == null || longitude == null) {
			log.error("location parameter array contains null values");
			throw new IllegalArgumentException("location parameter array contains null values");
		}
		if (latitude.isEmpty() || longitude.isEmpty()) {
			log.error("location parameter array contains empty values");
			throw new IllegalArgumentException("location parameter array contains empty values");
		}
		try {
			Double.parseDouble(latitude);
			Double.parseDouble(longitude);
		} catch (NumberFormatException e) {
			log.error("Failed to convert latitude/longitude strings to double values");
			throw new IllegalArgumentException("Error parsing latitude/longitude string to double values");
		}
		
		log.debug("exiting checkRequestParams(), all params are correct/valid");
	}
	
	private void putPosition(String jid, double latitude, double longitude) {
		// add data
		String selectSql = "SELECT * FROM users WHERE jid = ?";
		List<User> users = template.query(selectSql, new Object[] {jid}, new RowMapper<User>() {
			@Override
			public User mapRow(ResultSet rs, int rowNum) throws SQLException {
				return new User(rs.getInt("id"), rs.getString("jid"),
                        rs.getBoolean("online"));
			}
		});
		
		log.debug("Found {} existing users with jid={}", users.size(), jid);
		
		if (users.size() == 0) {
			int rows = 0;
			rows = template.update("INSERT INTO users(jid,online) values(?,?)", jid, "T");
			log.debug("Inserted {} rows to the users table", rows);
		}
		int rows = 0;		
		rows = template.update("INSERT INTO positions(user_id,latitude,longitude) VALUES((SELECT id FROM users WHERE jid = ?),?,?)", jid, latitude, longitude);
		log.debug("Inserted {} rows to the positions table", rows);
	}
	
	private List<User> getUsers() {
		List<User> results = template.query(
   				"SELECT * FROM users",
                new RowMapper<User>() {
                    @Override
                    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return new User(rs.getInt("id"), rs.getString("jid"),
                                rs.getBoolean("online"));
                    }
        });
		return results;
	}
	
	private List<Position> getPositions() {
   		List<Position> positions = template.query(
   				"SELECT p1.*,users.jid " +
   				"FROM positions p1 " +
   				"INNER JOIN users ON p1.user_id = users.id " +
   				"JOIN ( " +
   				" SELECT user_id, MAX(p2.timestamp) AS max_timestamp " +
   				" FROM positions p2 " +
   				" GROUP BY user_id " +
   				") p2 ON p2.user_id = p1.user_id AND p2.max_timestamp = p1.timestamp ",
   				new RowMapper<Position>() {
   					@Override
   					public Position mapRow(ResultSet rs, int rowNum) throws SQLException {
   						return new Position(rs.getInt("id"), rs.getDouble("latitude"), rs.getDouble("longitude"), 
   								            rs.getTimestamp("timestamp").getTime(), rs.getString("jid"));
   					}
   		});
   		return positions;
	}
	
	private List<Position> getAllPositions() {
		List<Position> positions = template.query(
   				"SELECT * FROM positions INNER JOIN users ON "
   				+ "positions.user_id = users.id ORDER BY positions.user_id",
   				new RowMapper<Position>() {
   					@Override
   					public Position mapRow(ResultSet rs, int rowNum) throws SQLException {
   						return new Position(rs.getInt("id"), rs.getDouble("latitude"), rs.getDouble("longitude"), 
   								            rs.getTimestamp("timestamp").getTime(), 
   								            rs.getString("jid"));
   					}
   		});
		return positions;
	}
	
	
	@Override
	public void destroy() {
		// This manually unregisters the JDBC driver
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
                log.debug("Unregistered the jdbc driver: {} ", driver);
            } catch (SQLException e) {
                log.error("Error while unregistering the driver {} ", driver);
            }
        }
	}
	
	// Must call from all public and protected instance methods
	private void checkInit() {
		if (init.get() != State.INITIALIZED)
			throw new IllegalStateException("Uninitialized");
	}
	
	public static void main(String[] args) {
		throw new AssertionError("Not supposed to be called");
	}

}
