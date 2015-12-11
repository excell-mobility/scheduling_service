package utility;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

import utility.DateAnalyser;

public class TestDateAnalyser {

	@Test
	public void testDurationCalculation() {
		
		Date start = new GregorianCalendar(2015, 11, 10, 14, 00).getTime();
		Date end = new GregorianCalendar(2015, 11, 10, 15, 00).getTime();
		assertEquals(60, DateAnalyser.getDurationBetweenDates(start, end));
		
	}
	
	@Test
	public void testWrongAppointment() {
		
		Date start = new GregorianCalendar(2015, 11, 10, 15, 00).getTime();
		Date end = new GregorianCalendar(2015, 11, 10, 13, 00).getTime();
		assertEquals(0, DateAnalyser.getDurationBetweenDates(start, end));
		
	}
	
	@Test
	public void testGetEarliestPossibleStartingDate() {
		
		Date date = new GregorianCalendar(2015, 11, 10, 15, 00).getTime();
		int travelTime = 1200000;
		assertTrue(new GregorianCalendar(2015, 11, 10, 15, 20).getTime().equals(
				DateAnalyser.getEarliestPossibleStartingDate(date, travelTime, true)));
		assertFalse(new GregorianCalendar(2015, 11, 10, 15, 10).getTime().equals(
				DateAnalyser.getEarliestPossibleStartingDate(date, travelTime, true)));
		
		travelTime = 20;
		assertTrue(new GregorianCalendar(2015, 11, 10, 15, 20).getTime().equals(
				DateAnalyser.getEarliestPossibleStartingDate(date, travelTime, false)));
		assertFalse(new GregorianCalendar(2015, 11, 10, 15, 10).getTime().equals(
				DateAnalyser.getEarliestPossibleStartingDate(date, travelTime, false)));
		
	}
	
	@Test
	public void testGetLatestPossibleEndDate() {
		
		Date date = new GregorianCalendar(2015, 11, 10, 15, 00).getTime();
		int travelTime = 1200000;
		assertTrue(new GregorianCalendar(2015, 11, 10, 14, 40).getTime().equals(
				DateAnalyser.getLatestPossibleEndDate(date, travelTime, true)));
		assertFalse(new GregorianCalendar(2015, 11, 10, 14, 50).getTime().equals(
				DateAnalyser.getLatestPossibleEndDate(date, travelTime, true)));
		
		travelTime = 20;
		assertTrue(new GregorianCalendar(2015, 11, 10, 14, 40).getTime().equals(
				DateAnalyser.getLatestPossibleEndDate(date, travelTime, false)));
		assertFalse(new GregorianCalendar(2015, 11, 10, 14, 50).getTime().equals(
				DateAnalyser.getLatestPossibleEndDate(date, travelTime, false)));
		
	}

}
