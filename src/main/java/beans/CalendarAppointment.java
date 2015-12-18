package beans;

import java.util.Date;

public class CalendarAppointment extends Appointment {

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
		return "CalendarAppointment [id=" + id + "]";
	}

}