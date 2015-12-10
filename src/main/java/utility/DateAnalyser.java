package utility;

import java.util.Date;

public class DateAnalyser {
	
	public static int getDurationBetweenDates(Date startDate, Date endDate) {
		
		if(startDate.before(endDate)) {
			return (int) ((endDate.getTime() - startDate.getTime()) / 1000);
		} else {
			return 0;
		}
		
	}

}