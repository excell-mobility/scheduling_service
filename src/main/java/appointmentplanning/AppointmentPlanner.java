package appointmentplanning;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;

import optimizer.TourOptimizer;

import org.json.JSONException;
import org.json.simple.JSONObject;

import beans.Appointment;
import beans.GeoPoint;
import beans.Timeslot;

import com.google.common.collect.Lists;

public class AppointmentPlanner {
	
	private TourOptimizer optimizer;

	public AppointmentPlanner() {
		optimizer = new TourOptimizer(null);
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject startPlanning(Integer year, Integer month, Integer day, 
			Integer durationOfAppointmentInMin, Double appointmentLat, Double appointmentLon) {
		
		JSONObject obj = new JSONObject();
		
		// TODO get possible appointments from calendar service
		List<Appointment> appointments = Lists.newArrayList();
		appointments.add(new Appointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime()));
		appointments.add(new Appointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), new GregorianCalendar(2015, 11, 10, 13, 00).getTime()));
		appointments.add(new Appointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 14, 00).getTime(), new GregorianCalendar(2015, 11, 10, 15, 00).getTime()));
		appointments.add(new Appointment(new GeoPoint(51.038104, 13.775029),
				new GregorianCalendar(2015, 11, 10, 17, 00).getTime(), new GregorianCalendar(2015, 11, 10, 18, 00).getTime()));
		
		// find a possible time slot
		optimizer.setAppointments(appointments);
		GeoPoint appointmentLocation = new GeoPoint(appointmentLat, appointmentLon);
		Timeslot timeslot = null;
		try {
			timeslot = optimizer.
					getPossibleTimeslotForNewAppointment(appointmentLocation, durationOfAppointmentInMin);
		} catch (JSONException | IOException e) {
			obj.put("Error", "No appointment planning possible!");
		}
		
		// check the result from tour optimization
		if(timeslot == null) {
			obj.put("Error", "No appointment planning possible!");
		} else {
			obj.put("startTimeslot", timeslot.getStartDate());
			obj.put("endTimeslot", timeslot.getEndDate());
		}
		
		// return the time slot or error message
		return obj;
		
	}

}
 