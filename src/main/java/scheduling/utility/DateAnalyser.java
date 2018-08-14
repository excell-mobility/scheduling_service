package scheduling.utility;

import java.time.ZoneId;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateAnalyser {
	
	public static int getDurationBetweenDates(Date startDate, Date endDate) {
		
		if(startDate.before(endDate)) {
			return MeasureConverter.getTimeInMinutes((int)(endDate.getTime() - startDate.getTime()));
		} else {
			return 0;
		}
		
	}
	
	public static Date getEarliestPossibleStartingDate(Date previousDate, int travelTime, 
			boolean convertIntoRoundedMinutes) {
		
		int minutes;
		if(convertIntoRoundedMinutes) {
			minutes = MeasureConverter.getTimeInMinutes(travelTime);
		} else {
			minutes = travelTime;
		}
		long newTimestamp = previousDate.getTime() + minutes * 60 * 1000;
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(newTimestamp);
		calendar.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));
		return calendar.getTime();
	}
	
	public static Date getLatestPossibleEndDate(Date followingDate, int travelTime,
			boolean convertIntoRoundedMinutes) {
		
		int minutes;
		if(convertIntoRoundedMinutes) {
			minutes = MeasureConverter.getTimeInMinutes(travelTime);
		} else {
			minutes = travelTime;
		}
		long newTimestamp = followingDate.getTime() - minutes * 60 * 1000;
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(newTimestamp);
		calendar.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Europe/Berlin")));
		return calendar.getTime();
	}

}