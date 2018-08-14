package scheduling.beans;

import java.io.Serializable;
import java.util.List;

public class Vehicle implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3819586916312066497L;
	
	private String vehicleID;
	private List<String> skills;
	private int earliestStart;
	private int latestArrival;
	private int breakStartWindow;
	private int breakEndWindow;
	private int breakTime;
	
	public Vehicle(String vehicleID, List<String> skills, int earliestStart,
			int latestArrival, int breakStartWindow, int breakEndWindow,
			int breakTime) {
		this.vehicleID = vehicleID;
		this.skills = skills;
		this.earliestStart = earliestStart;
		this.latestArrival = latestArrival;
		this.breakStartWindow = breakStartWindow;
		this.breakEndWindow = breakEndWindow;
		this.breakTime = breakTime;
	}
	
	public String getVehicleID() {
		return vehicleID;
	}
	public void setVehicleID(String vehicleID) {
		this.vehicleID = vehicleID;
	}
	public List<String> getSkills() {
		return skills;
	}
	public void setSkills(List<String> skills) {
		this.skills = skills;
	}
	public int getEarliestStart() {
		return earliestStart;
	}
	public void setEarliestStart(int earliestStart) {
		this.earliestStart = earliestStart;
	}
	public int getLatestArrival() {
		return latestArrival;
	}
	public void setLatestArrival(int latestArrival) {
		this.latestArrival = latestArrival;
	}
	public int getBreakStartWindow() {
		return breakStartWindow;
	}
	public void setBreakStartWindow(int breakStartWindow) {
		this.breakStartWindow = breakStartWindow;
	}
	public int getBreakEndWindow() {
		return breakEndWindow;
	}
	public void setBreakEndWindow(int breakEndWindow) {
		this.breakEndWindow = breakEndWindow;
	}
	public int getBreakTime() {
		return breakTime;
	}
	public void setBreakTime(int breakTime) {
		this.breakTime = breakTime;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
	
	@Override
	public String toString() {
		return "Vehicle [vehicleID=" + vehicleID + ", skills=" + skills
				+ ", earliestStart=" + earliestStart + ", latestArrival="
				+ latestArrival + ", breakStartWindow=" + breakStartWindow
				+ ", breakEndWindow=" + breakEndWindow + ", breakTime="
				+ breakTime + "]";
	}

}
