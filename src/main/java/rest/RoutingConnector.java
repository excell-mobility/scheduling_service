package rest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import beans.GeoPoint;
import utility.PropertyReader;

public class RoutingConnector {

	private static PropertyReader reader = new PropertyReader();

	public static synchronized int getTravelTime(GeoPoint start,
			GeoPoint end) throws JSONException, IOException {
		
		return getResultString(start, end).getInt("timeInMs");
		
	}
	
	public static synchronized double getTravelDistance(GeoPoint start,
			GeoPoint end) throws JSONException, IOException {
		
		return getResultString(start, end).getDouble("distance");
		
	}
	
	private static JSONObject getResultString(GeoPoint start,
			GeoPoint end) throws IOException {

		String 	urlStr = createUrlString(start, end);
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();

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
		JSONObject jsonObj = new JSONObject(sb.toString());
		return jsonObj;
	}

	private static String createUrlString(GeoPoint start,
			GeoPoint end) {
		String endPoint = reader.getPropertyValue("rest.routingendpoint");
		return endPoint + "startLat=" + start.getLatitude() + "&startLon=" + start.getLongitude() 
				+ "&endLat=" + end.getLatitude() + "&endLon=" + end.getLongitude();
	}

}
