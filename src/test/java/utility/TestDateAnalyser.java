package utility;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

import utility.DateAnalyser;

public class TestDateAnalyser {

	@Test
	public void testDurationCalculation() {
		
		Date start = new GregorianCalendar(2015, 11, 10, 14, 00).getTime();
		Date end = new GregorianCalendar(2015, 11, 10, 15, 00).getTime();
		assertEquals(3600, DateAnalyser.getDurationBetweenDates(start, end));
		
	}
	
	@Test
	public void testWrongAppointment() {
		
		Date start = new GregorianCalendar(2015, 11, 10, 15, 00).getTime();
		Date end = new GregorianCalendar(2015, 11, 10, 13, 00).getTime();
		assertEquals(0, DateAnalyser.getDurationBetweenDates(start, end));
		
	}

}
