package optimizer;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import beans.Appointment;
import beans.GeoPoint;

import com.google.common.collect.Lists;

public class TestOptimizer {
	
	List<Appointment> appointments;
	
	@Before
	public void createDatastructures() {
		
		appointments = Lists.newArrayList();
		appointments.add(new Appointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime()));
		appointments.add(new Appointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), new GregorianCalendar(2015, 11, 13, 10, 00).getTime()));
		appointments.add(new Appointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 14, 00).getTime(), new GregorianCalendar(2015, 11, 15, 10, 00).getTime()));
		appointments.add(new Appointment(new GeoPoint(51.038104, 13.775029),
				new GregorianCalendar(2015, 11, 10, 17, 00).getTime(), new GregorianCalendar(2015, 11, 18, 10, 00).getTime()));
	}

	@Test
	public void testAppointmentOptimization() {
		
		TourOptimizer optimizer = new TourOptimizer(appointments);
		Appointment appointment = new Appointment(new GeoPoint(51.030306, 13.730407), null, null);
		assertTrue(optimizer.findTimeslotForNewAppointment(appointment));
		
		Date newDate = new GregorianCalendar(2015, 11, 10, 14, 00).getTime();
		assertEquals(0, newDate.compareTo(optimizer.getPossibleStartdateForNewAppointment(appointment)));
		
	}

}
