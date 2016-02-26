package scheduling.model;

import beans.Timeslot;

public class PlanningResponse {

	private int travelTime;
	private Timeslot timeslot;
	private String calendarId;
	
	// dummy constructor
	public PlanningResponse(){
		
	}
	
	public PlanningResponse(int travelTime, Timeslot timeslot, String calendarId) {
		this.setTravelTime(travelTime);
		this.setTimeslot(timeslot);
		this.setCalendarId(calendarId);
	}

	public int getTravelTime() {
		return travelTime;
	}

	public void setTravelTime(int travelTime) {
		this.travelTime = travelTime;
	}
	
	public Timeslot getTimeslot() {
		return timeslot;
	}

	public void setTimeslot(Timeslot timeslot) {
		this.timeslot = timeslot;
	}

	public String getCalendarId() {
		return calendarId;
	}

	public void setCalendarId(String calendarId) {
		this.calendarId = calendarId;
	}
	
}
