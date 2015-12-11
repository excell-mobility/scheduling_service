package utility;

import java.util.Date;
import java.util.GregorianCalendar;

public class DateAnalyser {
	
	public static int getDurationBetweenDates(Date startDate, Date endDate) {
		
		if(startDate.before(endDate)) {
			return MeasureConverter.getTimeInMinutes((int)(endDate.getTime() - startDate.getTime()));
		} else {
			return 0;
		}
		
	}
	
	public static Date getEarliestPossibleStartingDate(Date endDate, int travelTime, 
			boolean convertIntoRoundedMinutes) {
		
		int minutes;
		if(convertIntoRoundedMinutes) {
			minutes = MeasureConverter.getTimeInMinutes(travelTime);
		} else {
			minutes = travelTime;
		}
		long newTimestamp = endDate.getTime() + minutes * 60 * 1000;
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(newTimestamp);
		return calendar.getTime();
		
	}
	
	public static Date getLatestPossibleEndDate(Date startDate, int travelTime,
			boolean convertIntoRoundedMinutes) {
		
		int minutes;
		if(convertIntoRoundedMinutes) {
			minutes = MeasureConverter.getTimeInMinutes(travelTime);
		} else {
			minutes = travelTime;
		}
		long newTimestamp = startDate.getTime() - minutes * 60 * 1000;
		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(newTimestamp);
		return calendar.getTime();
		
	}

}