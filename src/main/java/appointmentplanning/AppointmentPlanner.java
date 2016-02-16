package appointmentplanning;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import optimizer.TourOptimizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.simple.JSONObject;

import rest.CalendarConnector;
import rest.RoutingConnector;
import utility.DateAnalyser;
import utility.MeasureConverter;
import beans.CalendarAppointment;
import beans.GeoPoint;
import beans.Timeslot;
import beans.WorkingDay;

import com.google.common.collect.Maps;

import extraction.AppointmentExtraction;

public class AppointmentPlanner {
	
	private TourOptimizer optimizer;

	public AppointmentPlanner() {
		optimizer = new TourOptimizer(null);
	}
	
	@SuppressWarnings("unchecked")
	public JSONObject startPlanning(Integer year, Integer month, Integer day, 
			Integer durationOfAppointmentInMin, Double appointmentLat, Double appointmentLon) {
		
		JSONObject obj = new JSONObject();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEEEEEE", Locale.ENGLISH);
		String appointmentWeekDay = dateFormat.
				format(new GregorianCalendar(year, month, day).getTime()).toLowerCase();
		
		Timeslot timeslot = null;
		try {
			// get possible appointments from calendar service
			JSONArray appointmentsForCalendar = CalendarConnector.getAppointmentsForCalendar("test");
			AppointmentExtraction appointmentExtraction = new AppointmentExtraction();
			List<CalendarAppointment> appointments = appointmentExtraction.extractAppointments(appointmentsForCalendar);
			
			// find a possible time slot
			optimizer.setAppointments(appointments);
			GeoPoint appointmentLocation = new GeoPoint(appointmentLat, appointmentLon);
			
			Map<String, WorkingDay> workingDays = Maps.newHashMap();
			// extract working hours from calendar service, working hours are needed, 
			// if there is not enough time between the appointments
			org.json.JSONObject workingHoursForCalendar = 
					CalendarConnector.getWorkingHoursForCalendar("test");
			workingDays = appointmentExtraction.extractWorkingHours(workingHoursForCalendar);
			
			timeslot = optimizer.
					getPossibleTimeslotForNewAppointment(appointmentLocation, durationOfAppointmentInMin);
			// check, if it is possible to put the new appointment at the beginning or end
			if(timeslot == null && appointments != null 
					&& appointments.size() > 0) {
				// try to insert the appointment at the beginning
				WorkingDay workingDay = workingDays.get(appointmentWeekDay);
				if(workingDay != null) {
					Date beginningDate = new GregorianCalendar(year, month, day, 
							workingDay.getStartWorkingHour(), workingDay.getStartWorkingMinute()).getTime();
					Date endDate = new GregorianCalendar(year, month, day, 
							workingDay.getEndWorkingHour(), workingDay.getEndWorkingMinute()).getTime();
					Date latestEndDate = appointments.get(0).getStartDate();
					Date latestStartEndDate = appointments.get(appointments.size() - 1).getEndDate();
					int durationBetweenBeginningAndAppointment = DateAnalyser.
							getDurationBetweenDates(beginningDate, latestEndDate);
					int durationBetweenEndAndAppointment = DateAnalyser.
							getDurationBetweenDates(latestStartEndDate, endDate);
					if(durationBetweenBeginningAndAppointment > durationOfAppointmentInMin) {
						int travelTimeInMinutes = MeasureConverter.getTimeInMinutes(
								RoutingConnector.getTravelTime(appointmentLocation, appointments.get(0).getPosition()));
						// add the travel time and create a new time slot
						if((durationBetweenBeginningAndAppointment - travelTimeInMinutes)
								> durationOfAppointmentInMin) {
							timeslot = new Timeslot(beginningDate, 
									DateAnalyser.getLatestPossibleEndDate(latestEndDate, 
											travelTimeInMinutes, false));
						}
					} 
					// try to insert the appoint at the end
					if (timeslot == null && (durationBetweenEndAndAppointment > durationOfAppointmentInMin)) {
						int travelTimeInMinutes = MeasureConverter.getTimeInMinutes(
								RoutingConnector.getTravelTime(appointments.get(appointments.size() - 1).getPosition(), 
										appointmentLocation));
						// add the travel time and create a new time slot
						if((durationBetweenEndAndAppointment - travelTimeInMinutes)
								> durationOfAppointmentInMin) {
							timeslot = new Timeslot(DateAnalyser.getEarliestPossibleStartingDate(
									latestStartEndDate, travelTimeInMinutes, false), endDate);
						}
					}
				}
				
			}
		} catch (JSONException | IOException | ParseException e) {
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
 