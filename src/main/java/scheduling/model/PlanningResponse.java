package scheduling.model;

import beans.Timeslot;

public class PlanningResponse {

	private int travelTime;
	private double travelDistance;
	private Timeslot timeslot;
	private String calendarId;
	
	// dummy constructor
	public PlanningResponse(){
		
	}
	
	public PlanningResponse(int travelTime, double travelDistance, Timeslot timeslot, String calendarId) {
		this.setTravelTime(travelTime);
		this.setTravelDistance(travelDistance);
		this.setTimeslot(timeslot);
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
