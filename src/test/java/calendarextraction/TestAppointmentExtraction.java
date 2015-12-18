package calendarextraction;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import beans.CalendarAppointment;
import beans.WorkingDay;

public class TestAppointmentExtraction {

	private JSONArray jsonUsers;
	private JSONArray jsonAppointments;
	private JSONObject jsonWorkingHours;

	@Before
	public void initializeCalendarUsers() throws IOException {

		BufferedReader br = new BufferedReader(new FileReader(
				"src/test/resources/calendarusers.json"));
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();

		while (line != null) {
			sb.append(line);
			sb.append(System.lineSeparator());
			line = br.readLine();
		}
		br.close();
		jsonUsers = new JSONArray(sb.toString());

	}
	
	@Before
	public void initializeWorkingHours() throws IOException {

		BufferedReader br = new BufferedReader(new FileReader(
				"src/test/resources/workinghours.json"));
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();

		while (line != null) {
			sb.append(line);
			sb.append(System.lineSeparator());
			line = br.readLine();
		}
		br.close();
		jsonWorkingHours = new JSONObject(sb.toString());

	}
	
	@Before
	public void initializeAppointments() throws IOException {

		BufferedReader br = new BufferedReader(new FileReader(
				"src/test/resources/appointments.json"));
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();

		while (line != null) {
			sb.append(line);
			sb.append(System.lineSeparator());
			line = br.readLine();
		}
		br.close();
		jsonAppointments = new JSONArray(sb.toString());

	}

	@Test
	public void testCalendarUsersExtraction() throws IOException {

		AppointmentExtraction extraction = new AppointmentExtraction();
		assertTrue(extraction.extractCalendarUsers(jsonUsers).size() > 0);

	}
	
	@Test
	public void testWorkingHoursExtraction() throws IOException {

		AppointmentExtraction extraction = new AppointmentExtraction();
		Map<String, WorkingDay> extractWorkingHours = extraction.extractWorkingHours(jsonWorkingHours);
		assertEquals(2, extractWorkingHours.size());
		assertTrue(extractWorkingHours.containsKey("monday"));
		assertTrue(extractWorkingHours.containsKey("wednesday"));
	
		assertEquals(9, extractWorkingHours.get("monday").getStartWorkingHour());
		assertEquals(14, extractWorkingHours.get("monday").getEndWorkingHour());
		assertEquals(8, extractWorkingHours.get("wednesday").getStartWorkingHour());
		assertEquals(18, extractWorkingHours.get("wednesday").getEndWorkingHour());
		
		assertEquals(12, extractWorkingHours.get("monday").getStartBreakHour());
		assertEquals(14, extractWorkingHours.get("monday").getEndBreakHour());
		assertEquals(12, extractWorkingHours.get("wednesday").getStartBreakHour());
		assertEquals(14, extractWorkingHours.get("wednesday").getEndBreakHour());

	}
	
	@Test
	public void testAppointmentExtraction() throws IOException, ParseException {
		
		AppointmentExtraction extraction = new AppointmentExtraction();
		List<CalendarAppointment> extractAppointments = extraction.extractAppointments(jsonAppointments);
		assertEquals(1, extractAppointments.size());
		CalendarAppointment appointment = extractAppointments.get(0);
		assertTrue(appointment.getStartDate().before(appointment.getEndDate()));
		
	}

}
