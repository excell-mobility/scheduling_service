package beans;

import java.util.Date;

public class Timeslot {
	
	private Date startDate;
	private Date endDate;
	
	public Timeslot(Date startDate, Date endDate) {
		this.startDate = startDate;
		this.endDate = endDate;
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

	@Override
	public String toString() {
		return "Timeslot is from " + startDate + " to " + endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

}
