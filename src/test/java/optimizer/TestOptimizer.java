package optimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import beans.CalendarAppointment;
import beans.GeoPoint;
import scheduling.component.TourOptimizer;
import scheduling.model.PlanningResponse;

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
	public void testCheckOnlyTwoAppointments() throws Exception {
		
		TourOptimizer optimizer = new TourOptimizer();
		
		optimizer.setAppointments(appointments);
		appointments.remove(0);
		appointments.remove(0);
		List<PlanningResponse> possibleTimeslotForNewAppointment = 
				optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 60);
		assertEquals(new GregorianCalendar(2015, 11, 10, 15, 05).getTime(), 
				possibleTimeslotForNewAppointment.get(0).getTimeslot().getStartDate());
		assertEquals(new GregorianCalendar(2015, 11, 10, 16, 53).getTime(), 
				possibleTimeslotForNewAppointment.get(0).getTimeslot().getEndDate());
		possibleTimeslotForNewAppointment = 
				optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 109);
		assertTrue(possibleTimeslotForNewAppointment.isEmpty());
		
	}
	
	@Test
	public void testCheckTimeslotForNewAppointment() {
		
		TourOptimizer optimizer = new TourOptimizer();
		
		optimizer.setAppointments(appointments);
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
	public void testAppointmentOptimization() throws Exception {
		
		TourOptimizer optimizer = new TourOptimizer();
		optimizer.setAppointments(appointments);
		List<PlanningResponse> possibleTimeslotForNewAppointment = optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 30);
		assertEquals(new GregorianCalendar(2015, 11, 10, 15, 05).getTime(), 
				possibleTimeslotForNewAppointment.get(0).getTimeslot().getStartDate());
		assertEquals(new GregorianCalendar(2015, 11, 10, 16, 53).getTime(), 
				possibleTimeslotForNewAppointment.get(0).getTimeslot().getEndDate());
		
	}
	
	@Test
	public void testNullAppointmentOptimization() throws Exception {		
		
		appointments = Lists.newArrayList();
		appointments.add(new CalendarAppointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), "1"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), "2"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 12, 30).getTime(), new GregorianCalendar(2015, 11, 10, 15, 00).getTime(), "3"));
		TourOptimizer optimizer = new TourOptimizer();
		optimizer.setAppointments(appointments);
		assertTrue(optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 30).isEmpty());
		
		appointments = Lists.newArrayList();
		appointments.add(new CalendarAppointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), "1"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), "2"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 12, 40).getTime(), new GregorianCalendar(2015, 11, 10, 15, 00).getTime(), "3"));
		optimizer = new TourOptimizer();
		optimizer.setAppointments(appointments);
		assertTrue(optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 30).isEmpty());
		
	}

}
