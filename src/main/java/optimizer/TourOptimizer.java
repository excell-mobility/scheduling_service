package optimizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.json.JSONException;

import rest.RoutingConnector;
import utility.DateAnalyser;
import utility.MeasureConverter;
import beans.Appointment;
import beans.GeoPoint;
import beans.Timeslot;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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
	
	public boolean checkTimeslotForNewAppointment(Appointment appointment) {
		
		// check, if it is possible to include appointment in the list
		int durationOfAppointment = DateAnalyser.getDurationBetweenDates(
				appointment.getStartDate(), appointment.getEndDate());
		for(int index = 0; index < appointments.size() - 2; index++) {
			
			Appointment startAppointment = appointments.get(index);
			Appointment endAppointment = appointments.get(index + 1);
			int durationBetweenTwoAppointments = DateAnalyser.getDurationBetweenDates(
					startAppointment.getEndDate(), endAppointment.getStartDate());
			if(durationOfAppointment < durationBetweenTwoAppointments) {
				return true;
			}
			
		}
		return false;
		
	}

	public Timeslot getPossibleTimeslotForNewAppointment(GeoPoint location, int durationOfAppointmentInMin) throws JSONException, IOException {
		
		// return a valid starting date for the new appointment	
		TreeMap<Integer,Integer> timeIndexMapping = Maps.newTreeMap();
		HashMap<Integer,Integer> saveTravelTimesBefore = Maps.newHashMap();
		HashMap<Integer,Integer> saveTravelTimesAfter = Maps.newHashMap();
		
		// find insertion position
		for(int index = 0; index <= appointments.size() - 2; index++) {
			Appointment startAppointment = appointments.get(index);
			Appointment endAppointment = appointments.get(index + 1);
			int durationBetweenTwoAppointments = DateAnalyser.getDurationBetweenDates(
					startAppointment.getEndDate(), endAppointment.getStartDate());
			int travelTimeInMinutesBefore = MeasureConverter.getTimeInMinutes(
					RoutingConnector.getTravelTime(startAppointment.getPosition(), location));
			int travelTimeInMinutesAfter = MeasureConverter.getTimeInMinutes(
					RoutingConnector.getTravelTime(location, endAppointment.getPosition()));
			
			if((durationOfAppointmentInMin + travelTimeInMinutesBefore + travelTimeInMinutesAfter) 
					< durationBetweenTwoAppointments) {
				// calculate travel time of the whole route
				List<Appointment> newAppointments = Lists.newArrayList(appointments);
				newAppointments.add(index + 1, new Appointment(location, null, null));
				timeIndexMapping.put(calculateTravelTimes(newAppointments), index);
				// save travel times for calculation
				saveTravelTimesBefore.put(index, travelTimeInMinutesBefore);
				saveTravelTimesAfter.put(index, travelTimeInMinutesAfter);
			}
		}
		
		// extract the start date of new appointment
		if(saveTravelTimesBefore.size() > 0 
				&& timeIndexMapping.size() > 0
				&& saveTravelTimesAfter.size() > 0) {
			// the first value is the lowest travel time
			int index = timeIndexMapping.firstEntry().getValue();
			return new Timeslot(DateAnalyser.getEarliestPossibleStartingDate(appointments.get(index).getEndDate(), 
					saveTravelTimesBefore.get(index).intValue(), false), 
					DateAnalyser.getLatestPossibleEndDate(appointments.get(index + 1).getStartDate(), 
							saveTravelTimesAfter.get(index).intValue(), false));
		}
		return null;
		
	}

	private int calculateTravelTimes(List<Appointment> newAppointments) throws JSONException, IOException {
		
		int travelTimeSum = 0;
		for(int index = 0; index < newAppointments.size() - 2; index++) {
			travelTimeSum += MeasureConverter.getTimeInMinutes(
					RoutingConnector.getTravelTime(newAppointments.get(index).getPosition(), 
							newAppointments.get(index + 1).getPosition()));
		}
		return travelTimeSum;
		
	}
	
}