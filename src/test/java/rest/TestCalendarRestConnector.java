package rest;

import static org.junit.Assert.*;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class TestCalendarRestConnector {
	
	@Test
	public void testRESTCalendarUsers() throws JSONException, IOException {
		
		JSONArray json = CalendarConnector.getCalendarUsers();
		assertTrue(json.toString().length() > 2);

	}
	
	@Test
	public void testRESTCalendarAppointmentsForUser() throws JSONException, IOException {
		
		JSONArray json = CalendarConnector.getAppointmentsForCalendar("kalender");
		assertTrue(json.toString().length() > 2);

	}
	
	@Test
	public void testRESTCalendarWorkingHoursForUser() throws JSONException, IOException {
		
		JSONObject json = CalendarConnector.getWorkingHoursForCalendar("kalender");
		assertTrue(json.toString().length() > 2);

	}

}