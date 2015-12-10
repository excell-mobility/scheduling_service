package beans;

import static org.junit.Assert.assertEquals;

import java.util.GregorianCalendar;

import org.junit.Test;

public class TestAppointments {

	@Test
	public void testDurationCalculation() {
		
		GeoPoint start = new GeoPoint(51.5, 13.66);
		GeoPoint end = new GeoPoint(51.3, 13.76);
		Appointment appointment = new Appointment(start, end, 
				new GregorianCalendar(2015, 11, 10, 14, 00).getTime(), 
				new GregorianCalendar(2015, 11, 10, 15, 00).getTime());
		assertEquals(3600, appointment.getDurationOfAppointment());
		
	}
	
	@Test
	public void testWrongAppointment() {
		
		GeoPoint start = new GeoPoint(51.5, 13.66);
		GeoPoint end = new GeoPoint(51.3, 13.76);
		Appointment appointment = new Appointment(start, end, 
				new GregorianCalendar(2015, 11, 10, 15, 00).getTime(), 
				new GregorianCalendar(2015, 11, 10, 13, 00).getTime());
		assertEquals(0, appointment.getDurationOfAppointment());
		
	}

}
