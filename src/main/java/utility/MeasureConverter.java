package utility;

public class MeasureConverter {
	
	public static int getTimeInMinutes(int timeInMs) {	
		
		if(timeInMs > 0) {
			int saveTime = timeInMs / 1000;
			return (int)Math.ceil((double) saveTime / 60);
		} else {
			return 0;
		}
		
	}

}
