package beans;

import java.util.Date;

public class Appointment {
	
	private GeoPoint startPosition;
	private GeoPoint endPosition;
	private Date startDate;
	private Date endDate;
	
	public Appointment(GeoPoint startPosition, GeoPoint endPosition,
			Date startDate, Date endDate) {
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public GeoPoint getStartPosition() {
		return startPosition;
	}

	public void setStartPosition(GeoPoint startPosition) {
		this.startPosition = startPosition;
	}

	public GeoPoint getEndPosition() {
		return endPosition;
	}

	public void setEndPosition(GeoPoint endPosition) {
		this.endPosition = endPosition;
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
	
	public int getDurationOfAppointment() {
		
		if(startDate.before(endDate)) {
			return (int) ((endDate.getTime() - startDate.getTime()) / 1000);
		} else {
			return 0;
		}
		
	}

}