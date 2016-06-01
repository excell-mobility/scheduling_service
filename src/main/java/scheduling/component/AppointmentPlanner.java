package scheduling.component;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import beans.CalendarAppointment;
import beans.GeoPoint;
import beans.Timeslot;
import beans.WorkingDay;
import extraction.AppointmentExtraction;
import rest.CalendarConnector;
import rest.RoutingConnector;
import scheduling.model.PlanningResponse;
import utility.DateAnalyser;
import utility.MeasureConverter;

@Component
public class AppointmentPlanner {
	
	private final Logger log;
	private final CalendarConnector calendarConnector;
	private final RoutingConnector routingConnector;
	private final TourOptimizer optimizer;

	public AppointmentPlanner() {
	    this.log = LoggerFactory.getLogger(this.getClass());
		this.calendarConnector = new CalendarConnector();
		this.routingConnector = new RoutingConnector();
		optimizer = new TourOptimizer(routingConnector);
	}
	
	public List<PlanningResponse> startPlanning(Integer year, Integer month, Integer day, 
			Integer durationOfAppointmentInMin, Double appointmentLat, Double appointmentLon) {
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEEEEEE", Locale.ENGLISH);
		String appointmentWeekDay = dateFormat.
				format(new GregorianCalendar(year, month-1, day).getTime()).toLowerCase();
		
		List<PlanningResponse> planningList = Lists.newLinkedList();
		
		try {
			JSONArray calendarUsers = calendarConnector.getCalendarUsers();
			AppointmentExtraction extraction = new AppointmentExtraction();
			List<String> extractCalendarUsers = extraction.extractCalendarUsers(calendarUsers);
			
			// loop over users
			for(String calendarID : extractCalendarUsers) {

				AppointmentExtraction appointmentExtraction = new AppointmentExtraction();
				
				// create list for possible time slots per user
				List<PlanningResponse> timeslots = Lists.newLinkedList();
				
				// extract working hours from calendar service, working hours are needed, 
				// if there is not enough time between the appointments
				Map<String, WorkingDay> workingDays = Maps.newHashMap();
				org.json.JSONObject workingHoursForCalendar = 
						calendarConnector.getWorkingHoursForCalendar(calendarID);
				workingDays = appointmentExtraction.extractWorkingHours(workingHoursForCalendar);
				
				WorkingDay workingDay = workingDays.get(appointmentWeekDay);
				
				// check if user works on the selected day
				if (workingDay != null) {
					// get start and end position for user
					GeoPoint startPosition = new GeoPoint(51.030201,13.727380);
					GeoPoint endPosition = startPosition;
				
					// create appointment location
					GeoPoint appointmentLocation = new GeoPoint(appointmentLat, appointmentLon);
					
					// get start and end of workingHours and breakHours
					Date beginningDate = new GregorianCalendar(year, month-1, day, 
							workingDay.getStartWorkingHour(), workingDay.getStartWorkingMinute()).getTime();
					Date endDate = new GregorianCalendar(year, month-1, day, 
							workingDay.getEndWorkingHour(), workingDay.getEndWorkingMinute()).getTime();
					Date beginningBreak = new GregorianCalendar(year, month-1, day, 
							workingDay.getStartBreakHour(), workingDay.getStartBreakMinute()).getTime();
					Date endBreak = new GregorianCalendar(year, month-1, day, 
							workingDay.getEndBreakHour(), workingDay.getEndBreakMinute()).getTime();
					
					ZonedDateTime beginTime = beginningDate.toInstant().atZone(ZoneId.systemDefault());
					ZonedDateTime endTime = endDate.toInstant().atZone(ZoneId.systemDefault());
					
					beginTime.format(DateTimeFormatter.ISO_INSTANT);
					
					// construct time filter
					StringBuilder timeFilter = new StringBuilder("")
							.append("{\"begin\": \"").append(beginTime.format(DateTimeFormatter.ISO_INSTANT)).append("\",")
							.append("\"end\": \"").append(endTime.format(DateTimeFormatter.ISO_INSTANT)).append("\"}");
					
					// get appointments already set in calendar service
					JSONArray appointmentsForCalendar = calendarConnector.getAppointmentsForCalendar(calendarID, timeFilter.toString());
					List<CalendarAppointment> appointments = appointmentExtraction.extractAppointments(appointmentsForCalendar);
				
					// no appointments found, choose earliest date possible from working hours
					if (appointments == null || appointments.isEmpty()) {	
						// calculate travel distance for starting position of user to appointment
						int travelTimeInMinutes = MeasureConverter.getTimeInMinutes(
								routingConnector.getTravelTime(startPosition, appointmentLocation));
					
						double travelDistance = 
								routingConnector.getTravelDistance(startPosition, appointmentLocation);
								
						// get start time for first appointment incl. travel time
						Date beginFirstAppointment = DateAnalyser.getEarliestPossibleStartingDate(
								beginningDate, travelTimeInMinutes, false);
						
						Calendar calendar = Calendar.getInstance();
						calendar.setTime(beginFirstAppointment);
						calendar.add(Calendar.MINUTE, durationOfAppointmentInMin);
						Date endFirstAppointment = calendar.getTime();
						
						// only add time slot if duration of appointment does not exceed the end of the working day
						if (endFirstAppointment.before(endDate))
							timeslots.add(new PlanningResponse(
									travelTimeInMinutes * 2,
									travelDistance * 2,
									new Timeslot(beginFirstAppointment, endFirstAppointment),
									calendarID
									));
					}
					else {			
						// find a possible time slot
						optimizer.setAppointments(appointments);
						optimizer.setBeginWork(beginningDate);
						optimizer.setEndWork(endDate);
						optimizer.setBeginBreak(beginningBreak);
						optimizer.setEndBreak(endBreak);
						optimizer.setBeginLocation(startPosition);
						optimizer.setEndLocation(endPosition);
						optimizer.setCalendarId(calendarID);
						timeslots = optimizer.getPossibleTimeslotForNewAppointment(appointmentLocation, durationOfAppointmentInMin);
					}

					// add found time slots to planningList
					if (timeslots != null && !timeslots.isEmpty())
						planningList.addAll(timeslots);
				}
			}
		}
		catch (Exception e) {
			log.error("Error", "No appointment planning possible!");
		}
		
		// check the result from tour optimization
		/*if(planningList.isEmpty()) {
			Map<String, String> message = Maps.newHashMap();
			message.put("Error", "No appointment planning possible!");
			obj.put(0, message);
		} 
		else {
			// create json array and add appointments
			Integer id = 1;
			GeoPoint positionOfAppointment = new GeoPoint(appointmentLat, appointmentLon);
			for (PlanningResponse planning : planningList) {
				CalendarAppointment appointment = new CalendarAppointment(positionOfAppointment, 
						planning.getTimeslot().getStartDate(), DateAnalyser.
						getEarliestPossibleStartingDate(planning.getTimeslot().getStartDate(), 
								durationOfAppointmentInMin, false), id.toString());
				// json object is needed to create valid json
				JSONObject jsonObject = new JSONObject(appointment);
				obj.put(jsonObject);
				id++;
			}
		}*/
		
		// return the list of time slots or error message
		return planningList;
	}

}
 