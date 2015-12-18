package rest;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import utility.PropertyReader;

public class CalendarConnector {

	private static PropertyReader reader = new PropertyReader();

	public static synchronized JSONArray getCalendarUsers() throws JSONException, IOException {
		
		String urlString = reader.getPropertyValue("rest.calendarendpoint");
		return getJSONArrayResult(urlString, true);
		
	}
	
	public static synchronized JSONArray getAppointmentsForCalendar(String username) 
			throws JSONException, IOException {
		
		String urlString = reader.getPropertyValue("rest.calendarendpoint");
		urlString += "/" + username + "/appointments/searches";
		return getJSONArrayResult(urlString, false);
		
	}
	
	public static synchronized JSONObject getWorkingHoursForCalendar(String username) 
			throws JSONException, IOException {
		
		String urlString = reader.getPropertyValue("rest.calendarendpoint");
		urlString += "/" + username + "/workingHours";
		return getJSONObjectResult(urlString, true);
		
	}
	
	private static synchronized JSONObject getJSONObjectResult(String urlStr, boolean isGetRequest) throws IOException {
		
		String result = getConnectionString(urlStr, isGetRequest);
		JSONObject jsonObj = new JSONObject(result);
		return jsonObj;
		
	}
	
	private static synchronized String getConnectionString(String urlStr, boolean isGetRequest) throws MalformedURLException, IOException {
		
		HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
		if(isGetRequest) {
			conn.setRequestMethod("GET");
		} else {
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Accept", "application/json");
			// set up request body
			String urlParameters = "{}";
			// Send post request
			conn.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
		}

		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}

		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();

		conn.disconnect();
		return sb.toString();
		
	}
	
	private static synchronized JSONArray getJSONArrayResult(String urlStr, boolean isGetRequest) throws IOException {

		String result = getConnectionString(urlStr, isGetRequest);
		JSONArray jsonArray = new JSONArray(result);
		return jsonArray;
		
	}

}