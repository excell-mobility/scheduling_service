package optimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import beans.CalendarAppointment;
import beans.GeoPoint;
import beans.Timeslot;

import com.google.common.collect.Lists;

public class TestOptimizer {
	
	List<CalendarAppointment> appointments;
	
	@Before
	public void createDatastructures() {
		
		appointments = Lists.newArrayList();
		appointments.add(new CalendarAppointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), "1"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), new GregorianCalendar(2015, 11, 10, 13, 00).getTime(), "2"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 14, 00).getTime(), new GregorianCalendar(2015, 11, 10, 15, 00).getTime(), "3"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.038104, 13.775029),
				new GregorianCalendar(2015, 11, 10, 17, 00).getTime(), new GregorianCalendar(2015, 11, 10, 18, 00).getTime(), "4"));
	}
	
	@Test
	public void testCheckTimeslotForNewAppointment() {
		
		TourOptimizer optimizer = new TourOptimizer(appointments);
		CalendarAppointment appointment = new CalendarAppointment(
				new GeoPoint(51.030306, 13.730407), 
				new GregorianCalendar(2015, 11, 10, 16, 00).getTime(), 
				new GregorianCalendar(2015, 11, 10, 17, 00).getTime(), "1");
		assertTrue(optimizer.checkTimeslotForNewAppointment(appointment));
		appointments.remove(0);
		appointments.remove(appointments.size() - 1);
		assertFalse(optimizer.checkTimeslotForNewAppointment(appointment));
		
	}

	@Test
	public void testAppointmentOptimization() throws JSONException, IOException {
		
		TourOptimizer optimizer = new TourOptimizer(appointments);
		Timeslot possibleTimeslotForNewAppointment = optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 30);
		assertEquals(new GregorianCalendar(2015, 11, 10, 15, 05).getTime(), 
				possibleTimeslotForNewAppointment.getStartDate());
		assertEquals(new GregorianCalendar(2015, 11, 10, 16, 53).getTime(), 
				possibleTimeslotForNewAppointment.getEndDate());
		
	}
	
	@Test
	public void testNullAppointmentOptimization() throws JSONException, IOException {		
		
		appointments = Lists.newArrayList();
		appointments.add(new CalendarAppointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), "1"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), "2"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 12, 30).getTime(), new GregorianCalendar(2015, 11, 10, 15, 00).getTime(), "3"));
		TourOptimizer optimizer = new TourOptimizer(appointments);
		assertNull(optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 30));
		
		appointments = Lists.newArrayList();
		appointments.add(new CalendarAppointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), "1"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), "2"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 12, 40).getTime(), new GregorianCalendar(2015, 11, 10, 15, 00).getTime(), "3"));
		optimizer = new TourOptimizer(appointments);
		assertNull(optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 30));
		
	}

}
