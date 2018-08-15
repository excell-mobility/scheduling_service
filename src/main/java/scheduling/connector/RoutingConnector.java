package scheduling.connector;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import scheduling.beans.GeoPoint;
import scheduling.exceptions.RoutingNotFoundException;

@Component
public class RoutingConnector {

	private HttpConnector connector;

	@Value("${url.routingservice}")
	private String urlRoutingService;

	@Value("${url.routingservice.auth}")
	private boolean requiresToken;

	@Value("${url.authlayer}")
	private String urlAuthLayer;

	@Value("${url.authlayer.user}")
	private String urlAuthLayerUser;

	@Value("${url.authlayer.pw}")
	private String urlAuthLayerUserPw;

	public RoutingConnector() {
		this.connector = new HttpConnector();
	}

	public synchronized int getTravelTime(GeoPoint start,
			GeoPoint end) throws RoutingNotFoundException {
		String result = doRequest(createUrlString(start, end));
		if (result == null || result.equals(""))
			throw new RoutingNotFoundException("There's a problem with the connection");
		return this.connector.getJSONObjectResult(result).getInt("timeInMs");
		
	}

	public synchronized double getTravelDistance(GeoPoint start,
			GeoPoint end) throws RoutingNotFoundException {
		String result = doRequest(createUrlString(start, end));
		if (result == null || result.equals(""))
			throw new RoutingNotFoundException("There's a problem with the connection");
		return this.connector.getJSONObjectResult(result).getDouble("distance");
		
	}
	
	public synchronized List<Double[]> getGPSCoordinates(GeoPoint start,
			GeoPoint end) throws RoutingNotFoundException {
		
		List<Double[]> pointList = Lists.newLinkedList();
		
		try {
			String result = doRequest(createUrlString(start, end));
			if (result == null || result.equals(""))
				throw new RoutingNotFoundException("There's a problem with the connection");
			JSONArray jsonArray = this.connector.getJSONObjectResult(result).getJSONArray("pointList");
			
			for (int index = 0; index < jsonArray.length(); index++) {
				JSONArray points = jsonArray.getJSONArray(index);
				Double[] pointArray = new Double[2];
				pointArray[0] = points.getDouble(0);
				pointArray[1] = points.getDouble(1);
				pointList.add(pointArray);
			}
		} catch (Exception e) {
			//
		}
		return pointList;
		
	}
	
	public synchronized List<Double[]> getRoute(GeoPoint[] points) throws RoutingNotFoundException {
		
		List<Double[]> pointList = Lists.newLinkedList();
		
		for (int i = 0; i < points.length - 1; i++) {
			List<Double[]> stage = Lists.newLinkedList();
			stage = getGPSCoordinates(points[i], points[i+1]);
			
			if (stage == null || stage.isEmpty())
				return null;
			else {
				if (i == 0)
					pointList.addAll(stage);
				else {
					stage.remove(0);
					pointList.addAll(stage);
				}
			}
		}

		return pointList;
	}

	private String createUrlString(GeoPoint start,
			GeoPoint end) throws RoutingNotFoundException {
		if (start != null && end != null)
			return urlRoutingService + "?startLat=" + start.getLatitude() + "&startLon=" + start.getLongitude() 
					+ "&endLat=" + end.getLatitude() + "&endLon=" + end.getLongitude();
		else
			throw new RoutingNotFoundException("Coordinate missing. Can not perform routing!");
	}

	private String doRequest(String urlStr) throws RoutingNotFoundException{
		String result = null;
		try {
			if (requiresToken) {
				// get authentication token first
				String POST_PAYLOAD = "{" + "\"username\"" + ":" + "\"" + urlAuthLayerUser
						+ "\"" + "," + "\"password\"" + ":" + "\"" + urlAuthLayerUserPw + "\"" + "}";
				String token = "";

				try {
					String jsonResponse = connector.getConnectionAuthenticationString(urlAuthLayer,
							POST_PAYLOAD);
					JSONObject tokenJSON = new JSONObject(jsonResponse);
					if(tokenJSON.has("token")) {
						token = tokenJSON.getString("token");
					} else {
						return null;
					}
				} catch (KeyManagementException | NoSuchAlgorithmException| IOException e1) {
					e1.printStackTrace();
				}
				token = "Token " + token;

				try {
					result = connector.getConnectionStringWithToken(urlStr, token);
				} catch (KeyManagementException e) {
					e.printStackTrace();
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				} catch (MalformedURLException e) {
					e.printStackTrace();
				}
			}
			else {
				result = this.connector.getConnectionString(urlStr);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RoutingNotFoundException("Could not call routing service!");
		}
		
		return result;
	}
}
