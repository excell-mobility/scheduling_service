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
//import beans.Timeslot;
import beans.WorkingDay;
import extraction.AppointmentExtraction;
import rest.CalendarConnector;
import rest.IDMConnector;
import rest.RoutingConnector;
import rest.TrackingConnector;
import scheduling.model.PlanningResponse;
import utility.DateAnalyser;
import utility.MeasureConverter;

@Component
public class AppointmentPlanner {
	
	private final Logger log;
	private final CalendarConnector calendarConnector;
	private final RoutingConnector routingConnector;
 	private final TrackingConnector trackingConnector;
	private final IDMConnector idmConnector;
	private final TourOptimizer optimizer;

	public AppointmentPlanner() {
	    this.log = LoggerFactory.getLogger(this.getClass());
		this.calendarConnector = new CalendarConnector();
		this.routingConnector = new RoutingConnector();
 		this.trackingConnector = new TrackingConnector();
		optimizer = new TourOptimizer(routingConnector);
		this.idmConnector = new IDMConnector();
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
					// get start and end of workingHours and breakHours
					Date beginningDate = new GregorianCalendar(year, month-1, day, 
							workingDay.getStartWorkingHour(), workingDay.getStartWorkingMinute()).getTime();
					Date endDate = new GregorianCalendar(year, month-1, day, 
							workingDay.getEndWorkingHour(), workingDay.getEndWorkingMinute()).getTime();
					Date beginningBreak = new GregorianCalendar(year, month-1, day, 
							workingDay.getStartBreakHour(), workingDay.getStartBreakMinute()).getTime();
					Date endBreak = new GregorianCalendar(year, month-1, day, 
							workingDay.getEndBreakHour(), workingDay.getEndBreakMinute()).getTime();
					
					// as for now we assume that start and end of a staff member are the same
					GeoPoint endPosition = idmConnector.getGeoCoordByUserId(calendarID);
					
					// check if current time is already after extracted dates
					Date currentTime = new Date();
					if (currentTime.after(endDate))
						continue;
					
					// set position if currentTime is already after beginningDate
					GeoPoint startPosition = null;
					
					if (currentTime.after(beginningDate)) {
						beginningDate = currentTime;
						
						// get ID of tracking device from IDM
						String deviceId = idmConnector.extractDeviceIdOfUser(calendarID);
						
						// get current position of tracked device
						try {
							startPosition = trackingConnector.getCurrentPosition(deviceId);
							
							if (startPosition == null)
								startPosition = endPosition;
						}
						catch (Exception ex) {
							startPosition = endPosition;
						}
					}
					else
						// get start and end position for user
						// get the address as geoPoint from IDMConnector
						startPosition = endPosition;
					
					// no startingAddress set, go on to next staff member
					if (startPosition == null)
						continue;
					
					// prepare the timeFilter to query the calendar service
					ZonedDateTime beginTime = beginningDate.toInstant().atZone(ZoneId.of("GMT"));
					ZonedDateTime endTime = endDate.toInstant().atZone(ZoneId.of("GMT"));
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS+00:00");
//					LocalTime midnight = LocalTime.MIDNIGHT;
//					LocalDate today = LocalDate.now(ZoneId.of("Europe/Berlin"));
//					ZonedDateTime todayStart = ZonedDateTime.of(today, midnight, ZoneId.of("Europe/Berlin"));
//					String todayMidnight = todayStart.format(formatter);
//					String tomorrowMidnight = todayStart.plusDays(1).format(formatter);
					
					// construct time filter
					StringBuilder timeFilter = new StringBuilder("")
							.append("{\"begin\": \"").append(beginTime.format(formatter)).append("\",")
							.append("\"end\": \"").append(endTime.format(formatter)).append("\"}");
					
					// get appointments already set in calendar service
					JSONArray appointmentsForCalendar = calendarConnector.getAppointmentsForCalendar(calendarID, timeFilter.toString());
					List<CalendarAppointment> appointments = appointmentExtraction.extractAppointments(appointmentsForCalendar);

					// create appointment location
					GeoPoint appointmentLocation = new GeoPoint(appointmentLat, appointmentLon);
					
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
									beginFirstAppointment, 
									endFirstAppointment,
									//new Timeslot(beginFirstAppointment, endFirstAppointment),
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
 