package scheduling.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

//import beans.Timeslot;

public class PlanningResponse implements Comparable<PlanningResponse> {

	private int travelTime;
	private double travelDistance;
	private Date startDate;
	private Date endDate;
	private String calendarId;
	
	// dummy constructor
	public PlanningResponse(){
		
	}
	
	public PlanningResponse(int travelTime, 
			double travelDistance, 
			Date startDate, 
			Date endDate, 
			String calendarId) {
		this.setTravelTime(travelTime);
		this.setTravelDistance(travelDistance);
		this.startDate = startDate;
		this.endDate = endDate;
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

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS+00:00", timezone="GMT")
	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	@JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss.SSS+00:00", timezone="GMT")
	public Date getEndDate() {
		return endDate;
	}
	
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public String getCalendarId() {
		return calendarId;
	}

	public void setCalendarId(String calendarId) {
		this.calendarId = calendarId;
	}

	@Override
	public String toString() {
		return "PlanningResponse [travelTime=" + travelTime
				+ ", travelDistance=" + travelDistance + ", startDate="
				+ startDate + ", endDate=" + endDate + ", calendarId="
				+ calendarId + "]";
	}

	@Override
	public int compareTo(PlanningResponse arg0) {
		int traveltime1 = travelTime;
		int traveltime2 = arg0.travelTime;
		double travelDistance1 = travelDistance;
		double travelDistance2 = arg0.travelDistance;
		
		int comparison = Integer.compare(traveltime1, traveltime2);
		if(comparison == 0) {
			// travel time is the primary sorting object, therefore sort by distance now
			return Double.compare(travelDistance1, travelDistance2);
		} else {
			return comparison;
		}
		
	}

}
