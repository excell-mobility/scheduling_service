package optimizer;

import java.util.Date;
import java.util.List;

import beans.Appointment;

public class TourOptimizer {
	
	private List<Appointment> appointments;
	
	public TourOptimizer (List<Appointment> appointments) {
		this.appointments = appointments;
	}

	public List<Appointment> getAppointments() {
		return appointments;
	}

	public void setAppointments(List<Appointment> appointments) {
		this.appointments = appointments;
	}
	
	public boolean findTimeslotForNewAppointment(Appointment appointment) {
		
		// TODO
		return false;
		
	}

	public Date getPossibleStartdateForNewAppointment(Appointment appointment) {
		
		// TODO
		return null;
		
	}
	
}