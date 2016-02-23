package scheduling.component;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.JSONArray;
import org.springframework.stereotype.Component;

import rest.CalendarConnector;
import rest.RoutingConnector;
import utility.DateAnalyser;
import utility.MeasureConverter;
import beans.CalendarAppointment;
import beans.GeoPoint;
import beans.Timeslot;
import beans.WorkingDay;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import extraction.AppointmentExtraction;

@Component
public class AppointmentPlanner {
	
	private TourOptimizer optimizer;

	public AppointmentPlanner() {
		optimizer = new TourOptimizer(null);
	}
	
	@SuppressWarnings("unchecked")
	public JSONArray startPlanning(Integer year, Integer month, Integer day, 
			Integer durationOfAppointmentInMin, Double appointmentLat, Double appointmentLon) {
		
		JSONArray obj = new JSONArray();
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEEEEEE", Locale.ENGLISH);
		String appointmentWeekDay = dateFormat.
				format(new GregorianCalendar(year, month-1, day).getTime()).toLowerCase();
		
		List<Timeslot> timeslots = Lists.newLinkedList();
		try {
			// TODO change calendar identifier for CeBIT
			String calendarID = "test";
			// get possible appointments from calendar service
			org.json.JSONArray appointmentsForCalendar = CalendarConnector.getAppointmentsForCalendar(calendarID);
			AppointmentExtraction appointmentExtraction = new AppointmentExtraction();
			List<CalendarAppointment> appointments = appointmentExtraction.extractAppointments(appointmentsForCalendar);
			
			// find a possible time slot
			optimizer.setAppointments(appointments);
			GeoPoint appointmentLocation = new GeoPoint(appointmentLat, appointmentLon);
			
			Map<String, WorkingDay> workingDays = Maps.newHashMap();
			// extract working hours from calendar service, working hours are needed, 
			// if there is not enough time between the appointments
			org.json.JSONObject workingHoursForCalendar = 
					CalendarConnector.getWorkingHoursForCalendar(calendarID);
			workingDays = appointmentExtraction.extractWorkingHours(workingHoursForCalendar);
			
			timeslots = optimizer.
					getPossibleTimeslotForNewAppointment(appointmentLocation, durationOfAppointmentInMin);
			// check, if it is possible to put the new appointment at the beginning or end
			if(appointments != null 
					&& appointments.size() > 0) {
				// try to insert the appointment at the beginning
				WorkingDay workingDay = workingDays.get(appointmentWeekDay);
				if(workingDay != null) {
					Date beginningDate = new GregorianCalendar(year, month-1, day, 
							workingDay.getStartWorkingHour(), workingDay.getStartWorkingMinute()).getTime();
					Date endDate = new GregorianCalendar(year, month-1, day, 
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
							timeslots.add(new Timeslot(beginningDate, 
									DateAnalyser.getLatestPossibleEndDate(latestEndDate, 
											travelTimeInMinutes, false)));
						}
					} 
					// try to insert the appoint at the end
					if (durationBetweenEndAndAppointment > durationOfAppointmentInMin) {
						int travelTimeInMinutes = MeasureConverter.getTimeInMinutes(
								RoutingConnector.getTravelTime(appointments.get(appointments.size() - 1).getPosition(), 
										appointmentLocation));
						// add the travel time and create a new time slot
						if((durationBetweenEndAndAppointment - travelTimeInMinutes)
								> durationOfAppointmentInMin) {
							timeslots.add(new Timeslot(DateAnalyser.getEarliestPossibleStartingDate(
									latestStartEndDate, travelTimeInMinutes, false), endDate));
						}
					}
				}
				
			}
		} catch (JSONException | IOException | ParseException e) {
			Map<String, String> message = Maps.newHashMap();
			message.put("Error", "No appointment planning possible!");
			obj.add(0, message);
		}
		
		// check the result from tour optimization
		if(timeslots.isEmpty()) {
			Map<String, String> message = Maps.newHashMap();
			message.put("Error", "No appointment planning possible!");
			obj.add(0, message);
		} else {
			// create json array and add appointments
			Integer id = 1;
			GeoPoint positionOfAppointment = new GeoPoint(appointmentLat, appointmentLon);
			for(Timeslot timeslot: timeslots) {
				CalendarAppointment appointment = new CalendarAppointment(positionOfAppointment, 
						timeslot.getStartDate(), DateAnalyser.
						getEarliestPossibleStartingDate(timeslot.getStartDate(), 
								durationOfAppointmentInMin, false), id.toString());
				// json object is needed to create valid json
				JSONObject jsonObject = new JSONObject(appointment);
				obj.add(jsonObject);
				id++;
			}
		}
		// return the time slot or error message
		return obj;
		
	}

}
 