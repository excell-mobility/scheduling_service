package appointmentplanning;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import optimizer.TourOptimizer;

import org.json.JSONException;
import org.json.simple.JSONObject;

import rest.CalendarConnector;
import rest.RoutingConnector;
import utility.DateAnalyser;
import utility.MeasureConverter;
import beans.Appointment;
import beans.GeoPoint;
import beans.Timeslot;
import beans.WorkingDay;

import com.google.common.collect.Lists;
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
		
		// TODO get possible appointments from calendar service
		List<Appointment> appointments = Lists.newArrayList();
		appointments.add(new Appointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 11, 00).getTime()));
		appointments.add(new Appointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), new GregorianCalendar(2015, 11, 10, 13, 00).getTime()));
		appointments.add(new Appointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 14, 00).getTime(), new GregorianCalendar(2015, 11, 10, 16, 00).getTime()));
		appointments.add(new Appointment(new GeoPoint(51.038104, 13.775029),
				new GregorianCalendar(2015, 11, 10, 16, 30).getTime(), new GregorianCalendar(2015, 11, 10, 17, 00).getTime()));
		
		// find a possible time slot
		optimizer.setAppointments(appointments);
		GeoPoint appointmentLocation = new GeoPoint(appointmentLat, appointmentLon);
		Timeslot timeslot = null;
		try {
			Map<String, WorkingDay> workingDays = Maps.newHashMap();
			// extract working hours from calendar service, working hours are needed, 
			// if there is not enough time between the appointments
			org.json.JSONObject workingHoursForCalendar = 
					CalendarConnector.getWorkingHoursForCalendar("test");
			AppointmentExtraction appointmentExtraction = new AppointmentExtraction();
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
 