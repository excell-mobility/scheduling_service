package beans;

import java.util.Date;

public class Appointment {
	
	private GeoPoint position;
	private Date startDate;
	private Date endDate;
	
	public Appointment(GeoPoint position,
			Date startDate, Date endDate) {
		this.position = position;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public GeoPoint getPosition() {
		return position;
	}

	public void setPosition(GeoPoint position) {
		this.position = position;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

}