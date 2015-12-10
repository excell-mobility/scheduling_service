package rest;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import beans.GeoPoint;

public class TestRoutingRestConnection {
	
	@Test
	public void testRoutingRestAPIDistanceExtraction() throws IOException {
		
		GeoPoint start = new GeoPoint(51.048480, 13.729409);
		GeoPoint end = new GeoPoint(51.049660, 13.74);
		double result = RoutingConnector.getTravelDistance(start, end);
		assertEquals(980.0225623571259, result, 0.0000001);
		
	}
	
	@Test
	public void testRoutingRestAPITravelTimeExtraction() throws IOException {
		
		GeoPoint start = new GeoPoint(51.048480, 13.729409);
		GeoPoint end = new GeoPoint(51.049660, 13.74);
		int result = RoutingConnector.getTravelTime(start, end);
		assertEquals(154945, result);
		
	}

}