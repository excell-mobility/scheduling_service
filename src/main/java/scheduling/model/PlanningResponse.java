package scheduling.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

//import beans.Timeslot;

public class PlanningResponse {

	private int travelTime;
	private double travelDistance;
	private Date startDate;
	private Date endDate;
//	private Timeslot timeslot;
	private String calendarId;
	
	// dummy constructor
	public PlanningResponse(){
		
	}
	
	public PlanningResponse(int travelTime, 
			double travelDistance, 
			Date startDate, 
			Date endDate,
			//Timeslot timeslot, 
			String calendarId) {
		this.setTravelTime(travelTime);
		this.setTravelDistance(travelDistance);
		this.startDate = startDate;
		this.endDate = endDate;
//		this.setTimeslot(timeslot);
		this.setCalendarId(calendarId);
	}

	public int getTravelTime() {
		return travelTime;
	}
	
	public void setTravelTime(int travelTime) {
		this.travelTime = travelTime;
	}
	
	public double getTravelDistance() {
		return travelDistance;
	}

	public void setTravelDistance(double travelDistance) {
		this.travelDistance = travelDistance;
	}

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS+02:00", timezone="GMT")
	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS+02:00", timezone="GMT")
	public Date getEndDate() {
		return endDate;
	}
	
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
/*
	public Timeslot getTimeslot() {
		return timeslot;
	}

	public void setTimeslot(Timeslot timeslot) {
		this.timeslot = timeslot;
	}
*/
	public String getCalendarId() {
		return calendarId;
	}

	public void setCalendarId(String calendarId) {
		this.calendarId = calendarId;
	}
	
}
