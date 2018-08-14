package scheduling.beans;

import java.io.Serializable;
import java.util.List;

public class Service implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String serviceID;
	private TimeWindow timeWindow;
	private int serviceTime;
	private GeoPoint location;
	private List<String> requiredSkills;
	
	public Service(
			String serviceID,
			TimeWindow timeWindow,
			int serviceTime,
			GeoPoint location,
			List<String> requiredSkills) {
		super();
		this.serviceID = serviceID;
		this.timeWindow = timeWindow;
		this.serviceTime = serviceTime;
		this.location = location;
		this.requiredSkills = requiredSkills;
	}
	
	public String getServiceID() {
		return serviceID;
	}
	public void setServiceID(String serviceID) {
		this.serviceID = serviceID;
	}
	public TimeWindow getTimeWindow() {
		return timeWindow;
	}
	public void setTimeWindow(TimeWindow timeWindow) {
		this.timeWindow = timeWindow;
	}
	public int getServiceTime() {
		return serviceTime;
	}
	public void setServiceTime(int serviceTime) {
		this.serviceTime = serviceTime;
	}
	public GeoPoint getLocation() {
		return location;
	}
	public void setLocation(GeoPoint location) {
		this.location = location;
	}
	public List<String> getRequiredSkills() {
		return requiredSkills;
	}
	public void setRequiredSkills(List<String> requiredSkills) {
		this.requiredSkills = requiredSkills;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	@Override
	public String toString() {
		return "Service [serviceID=" + serviceID 
				+ ", timeWindow=" + timeWindow
				+ ", serviceTime=" + serviceTime
				+ ", location=" + location
				+ ", requiredSkills=" + requiredSkills + "]";
	}
	
}
