package scheduling.model;

import beans.Timeslot;

public class PlanningResponse {

	private Timeslot timeslot;
	private String calendarId;
	
	// dummy constructor
	public PlanningResponse(){
		
	}
	
	public PlanningResponse(Timeslot timeslot, String calendarId) {
		this.setTimeslot(timeslot);
		this.setCalendarId(calendarId);
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
