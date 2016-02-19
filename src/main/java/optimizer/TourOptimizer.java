package optimizer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeMap;

import org.json.JSONException;

import rest.RoutingConnector;
import utility.DateAnalyser;
import utility.MeasureConverter;
import beans.CalendarAppointment;
import beans.GeoPoint;
import beans.Timeslot;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TourOptimizer {
	
	private List<CalendarAppointment> appointments;
	
	public TourOptimizer (List<CalendarAppointment> appointments) {
		this.appointments = appointments;
	}

	public List<CalendarAppointment> getAppointments() {
		return appointments;
	}

	public void setAppointments(List<CalendarAppointment> appointments) {
		this.appointments = appointments;
	}
	
	public boolean checkTimeslotForNewAppointment(CalendarAppointment appointment) {
		
		// check, if it is possible to include appointment in the list
		int durationOfAppointment = DateAnalyser.getDurationBetweenDates(
				appointment.getStartDate(), appointment.getEndDate());
		for(int index = 0; index < appointments.size() - 2; index++) {
			
			CalendarAppointment startAppointment = appointments.get(index);
			CalendarAppointment endAppointment = appointments.get(index + 1);
			int durationBetweenTwoAppointments = DateAnalyser.getDurationBetweenDates(
					startAppointment.getEndDate(), endAppointment.getStartDate());
			if(durationOfAppointment < durationBetweenTwoAppointments) {
				return true;
			}
			
		}
		return false;
		
	}

	public List<Timeslot> getPossibleTimeslotForNewAppointment(GeoPoint location, int durationOfAppointmentInMin) throws JSONException, IOException {
		
		// return a valid starting date for the new appointment	
		TreeMap<Integer,Integer> timeIndexMapping = Maps.newTreeMap();
		HashMap<Integer,Integer> saveTravelTimesBefore = Maps.newHashMap();
		HashMap<Integer,Integer> saveTravelTimesAfter = Maps.newHashMap();

		// find insertion position
		for(int index = 0; index <= appointments.size() - 2; index++) {
			CalendarAppointment startAppointment = appointments.get(index);
			CalendarAppointment endAppointment = appointments.get(index + 1);
			int durationBetweenTwoAppointments = DateAnalyser.getDurationBetweenDates(
					startAppointment.getEndDate(), endAppointment.getStartDate());
			
			// check, if appointment duration is smaller than time between appointments
			if(durationOfAppointmentInMin >= durationBetweenTwoAppointments) {
				continue;
			}
			
			int travelTimeInMinutesBefore = MeasureConverter.getTimeInMinutes(
					RoutingConnector.getTravelTime(startAppointment.getPosition(), location));
			int travelTimeInMinutesAfter = MeasureConverter.getTimeInMinutes(
					RoutingConnector.getTravelTime(location, endAppointment.getPosition()));
			
			if((durationOfAppointmentInMin + travelTimeInMinutesBefore + travelTimeInMinutesAfter) 
					< durationBetweenTwoAppointments) {
				// calculate travel time of the whole route
				List<CalendarAppointment> newAppointments = Lists.newArrayList(appointments);
				newAppointments.add(index + 1, new CalendarAppointment(location, null, null, null));
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
			NavigableSet<Integer> descendingKeySet = timeIndexMapping.descendingKeySet().descendingSet();
			List<Timeslot> timeslots = Lists.newLinkedList();
			// add all possible time slots
			// first one is the possible time slot with the best travel time
			for(int index: descendingKeySet) {
				int valueIndex = timeIndexMapping.get(index);
				timeslots.add(new Timeslot(DateAnalyser.getEarliestPossibleStartingDate(appointments.get(valueIndex).getEndDate(), 
						saveTravelTimesBefore.get(valueIndex).intValue(), false), 
						DateAnalyser.getLatestPossibleEndDate(appointments.get(valueIndex + 1).getStartDate(), 
								saveTravelTimesAfter.get(valueIndex).intValue(), false)));
			}
			return timeslots;
		}
		// return empty list, if there are no valid appointments 
		return Lists.newLinkedList();
		
	}

	public int calculateTravelTimes(List<CalendarAppointment> newAppointments) throws JSONException, IOException {
		
		int travelTimeSum = 0;
		for(int index = 0; index < newAppointments.size() - 2; index++) {
			travelTimeSum += MeasureConverter.getTimeInMinutes(
					RoutingConnector.getTravelTime(newAppointments.get(index).getPosition(), 
							newAppointments.get(index + 1).getPosition()));
		}
		return travelTimeSum;
		
	}
	
}