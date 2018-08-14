package scheduling.beans;

import java.util.Date;

public class CalendarAppointment extends Appointment implements Comparable<CalendarAppointment> {

	private String id;
	
	public CalendarAppointment(GeoPoint position, Date startDate, Date endDate, String id) {
		super(position, startDate, endDate);
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return super.toString();
	}

	public int compareTo(CalendarAppointment o) {
		return this.getStartDate().compareTo(o.getStartDate());
	}
	
	

}