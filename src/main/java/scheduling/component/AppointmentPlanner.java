package scheduling.component;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import rest.CalendarConnector;
import rest.IDMConnector;
import rest.RoutingConnector;
import rest.TrackingConnector;
import scheduling.model.PlanningResponse;
import utility.DateAnalyser;
import utility.MeasureConverter;
import beans.CalendarAppointment;
import beans.GeoPoint;
import beans.JobConstraint;
import beans.JobProperties;
import beans.Service;
import beans.TransportProperties;
import beans.Vehicle;
//import beans.Timeslot;
import beans.WorkingDay;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager.Priority;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Break;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

import exceptions.InternalSchedulingErrorException;
import exceptions.RoutingNotFoundException;
import extraction.AppointmentExtraction;

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
			Integer durationOfAppointmentInMin, Double appointmentLat, Double appointmentLon) throws InternalSchedulingErrorException {
		
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
						
						try {
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
						catch (RoutingNotFoundException routeEx) {
							// no result for given coordinates - time slot will not be considered
						}
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
			throw new InternalSchedulingErrorException("No appointment planning possible!");
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
	
	public org.json.simple.JSONObject startPlanningPickup(JSONObject jsonObject) throws RoutingNotFoundException, InternalSchedulingErrorException {
		
		if(jsonObject == null 
				|| jsonObject.isNull("startLocation")
				|| jsonObject.isNull("vehicles")
				|| jsonObject.isNull("pickups")) {
			throw new InternalSchedulingErrorException("JSON is invalid, no scheduling possible for pickup scenario!");
		}
		
		// extract start location
		Location startingPoint = null;
		if(jsonObject.has("startLocation")) {
			JSONObject location = jsonObject.getJSONObject("startLocation");
			double latitude = location.has("latitude") ? location.getDouble("latitude") : 0.0;
			double longitude = location.has("longitude") ? location.getDouble("longitude") : 0.0;
			startingPoint = Location.newInstance(longitude, latitude);
		}
		
		// extract vehicle types
		Map<Integer, VehicleType> vehicleTypeMap = Maps.newHashMap();
		if(jsonObject.has("vehicles")) {
			
			JSONArray jsonArray = jsonObject.getJSONArray("vehicles");
			for(int index = 0; index < jsonArray.length(); index++) {
				JSONObject vehicleJSON = jsonArray.getJSONObject(index);
				int capacity = vehicleJSON.has("capacity") ? vehicleJSON.getInt("capacity") : 0;
				if(!vehicleTypeMap.containsKey(capacity)) {
			        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl
			        		.Builder
			        		.newInstance("Bus" + "_" + capacity)
			        		.addCapacityDimension(0, capacity);
			        VehicleType vehicleType = vehicleTypeBuilder.build();
			        vehicleTypeMap.put(capacity, vehicleType);
				}
			}
			
		}
		

		List<VehicleImpl> vehicles = null;
		if(jsonObject.has("vehicles")) {
			vehicles = Lists.newLinkedList();
			JSONArray jsonArray = jsonObject.getJSONArray("vehicles");
			for(int index = 0; index < jsonArray.length(); index++) {
				JSONObject vehicleJSON = jsonArray.getJSONObject(index);
				String vehicleID = vehicleJSON.has("vehicleID") ? vehicleJSON.getString("vehicleID") : "";
				double earliestStart = vehicleJSON.has("earliestStart") ? vehicleJSON.getInt("earliestStart") : 0;
				double latestArrival = vehicleJSON.has("latestArrival") ? vehicleJSON.getInt("latestArrival") : 0;
				int capacity = vehicleJSON.has("capacity") ? vehicleJSON.getInt("capacity") : 0;
				int breakStartWindow = 0;
				int breakEndWindow = 0;
				int breakTime = 0;
				if(vehicleJSON.has("break")) {
					JSONObject breakJSON = vehicleJSON.getJSONObject("break");
					breakStartWindow = breakJSON.has("startWindow") ? breakJSON.getInt("startWindow") : 0;
					breakEndWindow = breakJSON.has("endWindow") ? breakJSON.getInt("endWindow") : 0;
					breakTime = breakJSON.has("breakTime") ? breakJSON.getInt("breakTime") : 0;

				}
				Break driverBreak = Break.Builder.newInstance("Pause " + vehicleID)
	    				.setTimeWindow(TimeWindow.newInstance(breakStartWindow,
	    						breakEndWindow))
	    				.setServiceTime(breakTime)
	    				.setPriority(1)
	    				.build();
		        Builder vehicleBuilder = VehicleImpl.Builder.newInstance(vehicleID);
		        vehicleBuilder.setStartLocation(startingPoint);
		        vehicleBuilder.setType(vehicleTypeMap.get(capacity));
		        vehicleBuilder.setReturnToDepot(true);
		        vehicleBuilder.setEarliestStart(earliestStart);
		        vehicleBuilder.setLatestArrival(latestArrival);
		        if(breakTime != 0) {
		        	vehicleBuilder.setBreak(driverBreak);
		        }
		        VehicleImpl vehicle = vehicleBuilder.build();
				vehicles.add(vehicle);
			}
		}
		
		List<Shipment> pickups = null;
		if(jsonObject.has("pickups")) {
			pickups = Lists.newLinkedList();
			JSONArray jsonArray = jsonObject.getJSONArray("pickups");
			for(int index = 0; index < jsonArray.length(); index++) {
				JSONObject pickupJSON = jsonArray.getJSONObject(index);
				String pickupID = pickupJSON.has("pickupID") ? pickupJSON.getString("pickupID") : "";
				int pickupTime = pickupJSON.has("pickupTime") ? pickupJSON.getInt("pickupTime") : 0;
				int numberOfPassenger = pickupJSON.has("numberOfPersons") ? pickupJSON.getInt("numberOfPersons") : 0;
				int dropoffTime = pickupJSON.has("dropoffTime") ? pickupJSON.getInt("dropoffTime") : 0;
				double pickupLatitude = 0.0;
				double pickupLongitude = 0.0;
				double dropoffLatitude = 0.0;
				double dropoffLongitude = 0.0;
				double pickupStart = 0.0;
				double pickupEnd = 0.0;
				if(pickupJSON.has("locationpickup")) {
					JSONObject locationpickupJSON = pickupJSON.getJSONObject("locationpickup");
					pickupLatitude = locationpickupJSON.has("latitude") ? locationpickupJSON.getDouble("latitude") : 0.0;
					pickupLongitude = locationpickupJSON.has("longitude") ? locationpickupJSON.getDouble("longitude") : 0.0;
				}
				if(pickupJSON.has("locationdropoff")) {
					JSONObject locationdropoffJSON = pickupJSON.getJSONObject("locationdropoff");
					dropoffLatitude = locationdropoffJSON.has("latitude") ? locationdropoffJSON.getDouble("latitude") : 0.0;
					dropoffLongitude = locationdropoffJSON.has("longitude") ? locationdropoffJSON.getDouble("longitude") : 0.0;
				}
				if(pickupJSON.has("timeWindow")) {
					JSONObject timeJSON = pickupJSON.getJSONObject("timeWindow");
					pickupStart = timeJSON.has("start") ? timeJSON.getInt("start") : 0;
					pickupEnd = timeJSON.has("end") ? timeJSON.getInt("end") : 0;
				}
				pickups.add(Shipment.Builder.newInstance(pickupID)
						.addSizeDimension(0, numberOfPassenger)
						.setDeliveryServiceTime(dropoffTime)
						.setPickupServiceTime(pickupTime)
						.addPickupTimeWindow(new TimeWindow(pickupStart, pickupEnd))
						.setPickupLocation(Location.newInstance(
								pickupLongitude, 
								pickupLatitude))
		        		.setDeliveryLocation(Location.newInstance(
		        				dropoffLongitude, 
		        				dropoffLatitude))
		        		.build());
			}
		}

		VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix
				.Builder
				.newInstance(false);
		
		// create routing costs between shipments and starting point of the busses
    	for(Shipment shipment: pickups) {
    		
    		double travelDistanceStartPickup = 0.0;
    		int travelTimeStartPickup = 0;
    		double travelDistanceStartDelivery = 0.0;
    		int travelTimeStartDelivery = 0;
    		double travelDistancePickupStart = 0.0;
    		int travelTimePickupStart = 0;
    		double travelDistanceDeliveryStart = 0.0;
    		int travelTimeDeliveryStart = 0;
			try {
				
				// calculate costs from the starting point to delivery and pickup locations
				travelDistanceStartPickup = routingConnector.getTravelDistance(
						new GeoPoint(startingPoint.getCoordinate().getY(), 
								startingPoint.getCoordinate().getX()), 
						new GeoPoint(shipment.getPickupLocation().getCoordinate().getY(), 
								shipment.getPickupLocation().getCoordinate().getX()));
				travelTimeStartPickup = routingConnector.getTravelTime(
						new GeoPoint(startingPoint.getCoordinate().getY(), 
								startingPoint.getCoordinate().getX()), 
						new GeoPoint(shipment.getPickupLocation().getCoordinate().getY(), 
								shipment.getPickupLocation().getCoordinate().getX())) / 1000;
				travelDistanceStartDelivery = routingConnector.getTravelDistance(
						new GeoPoint(startingPoint.getCoordinate().getY(), 
								startingPoint.getCoordinate().getX()), 
						new GeoPoint(shipment.getDeliveryLocation().getCoordinate().getY(), 
								shipment.getDeliveryLocation().getCoordinate().getX()));
				travelTimeStartDelivery = routingConnector.getTravelTime(
						new GeoPoint(startingPoint.getCoordinate().getY(), 
								startingPoint.getCoordinate().getX()), 
						new GeoPoint(shipment.getDeliveryLocation().getCoordinate().getY(), 
								shipment.getDeliveryLocation().getCoordinate().getX())) / 1000;
				
				// calculate the costs from the delivery and pickup locations to the starting point
				travelDistancePickupStart = routingConnector.getTravelDistance( 
						new GeoPoint(shipment.getPickupLocation().getCoordinate().getY(), 
								shipment.getPickupLocation().getCoordinate().getX()),
						new GeoPoint(startingPoint.getCoordinate().getY(), 
								startingPoint.getCoordinate().getX()));
				travelTimePickupStart = routingConnector.getTravelTime(
						new GeoPoint(shipment.getPickupLocation().getCoordinate().getY(), 
								shipment.getPickupLocation().getCoordinate().getX()),
						new GeoPoint(startingPoint.getCoordinate().getY(), 
								startingPoint.getCoordinate().getX())) / 1000;
				travelDistanceDeliveryStart = routingConnector.getTravelDistance(
						new GeoPoint(shipment.getDeliveryLocation().getCoordinate().getY(), 
								shipment.getDeliveryLocation().getCoordinate().getX()),
						new GeoPoint(startingPoint.getCoordinate().getY(), 
								startingPoint.getCoordinate().getX()));
				travelTimeDeliveryStart = routingConnector.getTravelTime( 
						new GeoPoint(shipment.getDeliveryLocation().getCoordinate().getY(), 
								shipment.getDeliveryLocation().getCoordinate().getX()),
						new GeoPoint(startingPoint.getCoordinate().getY(), 
								startingPoint.getCoordinate().getX())) / 1000;
				
			} catch (Exception e) {
				throw new RoutingNotFoundException("Routing calculation from " 
						+ startingPoint
						+ " to service "
						+ shipment.getId()
						+ " not possible!");
			}

			// vehicles have all the same starting point
        	for(VehicleImpl vehicle: vehicles) {
        		costMatrixBuilder.addTransportDistance(vehicle.getId(), shipment.getId(), travelDistanceStartPickup);
        		costMatrixBuilder.addTransportTime(vehicle.getId(), shipment.getId(), travelTimeStartPickup);
        	}
        	
        	// set up the costs of a single shipment from the starting point
    		costMatrixBuilder.addTransportDistance(startingPoint.getId(), 
    				shipment.getPickupLocation().getId(), travelDistanceStartPickup);
    		costMatrixBuilder.addTransportTime(startingPoint.getId(), 
    				shipment.getPickupLocation().getId(), travelTimeStartPickup);
    		costMatrixBuilder.addTransportDistance(startingPoint.getId(), 
    				shipment.getDeliveryLocation().getId(), travelDistanceStartDelivery);
    		costMatrixBuilder.addTransportTime(startingPoint.getId(), 
    				shipment.getDeliveryLocation().getId(), travelTimeStartDelivery);
    		
    		// set up the costs of a single shipment to the starting point
    		costMatrixBuilder.addTransportDistance(shipment.getPickupLocation().getId(), 
    				startingPoint.getId(), travelDistancePickupStart);
    		costMatrixBuilder.addTransportTime(shipment.getPickupLocation().getId(),
    				startingPoint.getId(), travelTimePickupStart);
    		costMatrixBuilder.addTransportDistance(shipment.getDeliveryLocation().getId(),
    				startingPoint.getId(), travelDistanceDeliveryStart);
    		costMatrixBuilder.addTransportTime(shipment.getDeliveryLocation().getId(),
    				startingPoint.getId(), travelTimeDeliveryStart);

    	}
    	
    	// calculate costs between the shipments
        for(int i = 0; i < pickups.size(); i++) {
        	
        	for(int j = i + 1; j < pickups.size(); j++) {
        		double travelDistance = 0.0;
        		int travelTime = 0;
    			try {
    				// pickup i to j locations
    				travelDistance = routingConnector.getTravelDistance(
    						new GeoPoint(pickups.get(i).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(i).getPickupLocation().getCoordinate().getX()), 
    						new GeoPoint(pickups.get(j).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(j).getPickupLocation().getCoordinate().getX()));
    				travelTime = routingConnector.getTravelTime(
    						new GeoPoint(pickups.get(i).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(i).getPickupLocation().getCoordinate().getX()), 
							new GeoPoint(pickups.get(j).getPickupLocation().getCoordinate().getY(), 
									pickups.get(j).getPickupLocation().getCoordinate().getX())) / 1000;
            		costMatrixBuilder.addTransportDistance(pickups.get(i).getPickupLocation().getId(), 
            				pickups.get(j).getPickupLocation().getId(), travelDistance);
            		costMatrixBuilder.addTransportTime(pickups.get(i).getPickupLocation().getId(), 
            				pickups.get(j).getPickupLocation().getId(), travelTime);
            		// pickup j to i locations
    				travelDistance = routingConnector.getTravelDistance(
    						new GeoPoint(pickups.get(j).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(j).getPickupLocation().getCoordinate().getX()), 
    						new GeoPoint(pickups.get(i).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(i).getPickupLocation().getCoordinate().getX()));
    				travelTime = routingConnector.getTravelTime(
    						new GeoPoint(pickups.get(j).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(j).getPickupLocation().getCoordinate().getX()), 
							new GeoPoint(pickups.get(i).getPickupLocation().getCoordinate().getY(), 
									pickups.get(i).getPickupLocation().getCoordinate().getX())) / 1000;
            		costMatrixBuilder.addTransportDistance(pickups.get(j).getPickupLocation().getId(), 
            				pickups.get(i).getPickupLocation().getId(), travelDistance);
            		costMatrixBuilder.addTransportTime(pickups.get(j).getPickupLocation().getId(), 
            				pickups.get(i).getPickupLocation().getId(), travelTime);
    				// costs between pickup i and delivery j locations
    				travelDistance = routingConnector.getTravelDistance(
    						new GeoPoint(pickups.get(i).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(i).getPickupLocation().getCoordinate().getX()), 
    						new GeoPoint(pickups.get(j).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(j).getDeliveryLocation().getCoordinate().getX()));
    				travelTime = routingConnector.getTravelTime(
    						new GeoPoint(pickups.get(i).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(i).getPickupLocation().getCoordinate().getX()), 
							new GeoPoint(pickups.get(j).getDeliveryLocation().getCoordinate().getY(), 
									pickups.get(j).getDeliveryLocation().getCoordinate().getX())) / 1000;
            		costMatrixBuilder.addTransportDistance(pickups.get(i).getPickupLocation().getId(), 
            				pickups.get(j).getDeliveryLocation().getId(), travelDistance);
            		costMatrixBuilder.addTransportTime(pickups.get(i).getPickupLocation().getId(), 
            				pickups.get(j).getDeliveryLocation().getId(), travelTime);
    				// costs between pickup j and delivery i locations
    				travelDistance = routingConnector.getTravelDistance(
    						new GeoPoint(pickups.get(j).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(j).getPickupLocation().getCoordinate().getX()), 
    						new GeoPoint(pickups.get(i).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(i).getDeliveryLocation().getCoordinate().getX()));
    				travelTime = routingConnector.getTravelTime(
    						new GeoPoint(pickups.get(j).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(j).getPickupLocation().getCoordinate().getX()), 
							new GeoPoint(pickups.get(i).getDeliveryLocation().getCoordinate().getY(), 
									pickups.get(i).getDeliveryLocation().getCoordinate().getX())) / 1000;
            		costMatrixBuilder.addTransportDistance(pickups.get(j).getPickupLocation().getId(), 
            				pickups.get(i).getDeliveryLocation().getId(), travelDistance);
            		costMatrixBuilder.addTransportTime(pickups.get(j).getPickupLocation().getId(), 
            				pickups.get(i).getDeliveryLocation().getId(), travelTime);
    				// costs between delivery i and pickup j locations
    				travelDistance = routingConnector.getTravelDistance(
    						new GeoPoint(pickups.get(i).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(i).getDeliveryLocation().getCoordinate().getX()), 
    						new GeoPoint(pickups.get(j).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(j).getPickupLocation().getCoordinate().getX()));
    				travelTime = routingConnector.getTravelTime(
    						new GeoPoint(pickups.get(i).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(i).getDeliveryLocation().getCoordinate().getX()), 
							new GeoPoint(pickups.get(j).getPickupLocation().getCoordinate().getY(), 
									pickups.get(j).getPickupLocation().getCoordinate().getX())) / 1000;
            		costMatrixBuilder.addTransportDistance(pickups.get(i).getDeliveryLocation().getId(), 
            				pickups.get(j).getPickupLocation().getId(), travelDistance);
            		costMatrixBuilder.addTransportTime(pickups.get(i).getDeliveryLocation().getId(), 
            				pickups.get(j).getPickupLocation().getId(), travelTime);
       				// costs between delivery j and pickup i locations
    				travelDistance = routingConnector.getTravelDistance(
    						new GeoPoint(pickups.get(j).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(j).getDeliveryLocation().getCoordinate().getX()), 
    						new GeoPoint(pickups.get(i).getPickupLocation().getCoordinate().getY(), 
    								pickups.get(i).getPickupLocation().getCoordinate().getX()));
    				travelTime = routingConnector.getTravelTime(
    						new GeoPoint(pickups.get(j).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(j).getDeliveryLocation().getCoordinate().getX()), 
							new GeoPoint(pickups.get(i).getPickupLocation().getCoordinate().getY(), 
									pickups.get(i).getPickupLocation().getCoordinate().getX())) / 1000;
            		costMatrixBuilder.addTransportDistance(pickups.get(j).getDeliveryLocation().getId(), 
            				pickups.get(i).getPickupLocation().getId(), travelDistance);
            		costMatrixBuilder.addTransportTime(pickups.get(j).getDeliveryLocation().getId(), 
            				pickups.get(i).getPickupLocation().getId(), travelTime);
    				// costs between delivery i and delivery j locations
    				travelDistance = routingConnector.getTravelDistance(
    						new GeoPoint(pickups.get(i).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(i).getDeliveryLocation().getCoordinate().getX()), 
    						new GeoPoint(pickups.get(j).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(j).getDeliveryLocation().getCoordinate().getX()));
    				travelTime = routingConnector.getTravelTime(
    						new GeoPoint(pickups.get(i).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(i).getDeliveryLocation().getCoordinate().getX()), 
							new GeoPoint(pickups.get(j).getDeliveryLocation().getCoordinate().getY(), 
									pickups.get(j).getDeliveryLocation().getCoordinate().getX())) / 1000;
            		costMatrixBuilder.addTransportDistance(pickups.get(i).getDeliveryLocation().getId(), 
            				pickups.get(j).getDeliveryLocation().getId(), travelDistance);
            		costMatrixBuilder.addTransportTime(pickups.get(i).getDeliveryLocation().getId(), 
            				pickups.get(j).getDeliveryLocation().getId(), travelTime);
      				// costs between delivery j and delivery i locations
    				travelDistance = routingConnector.getTravelDistance(
    						new GeoPoint(pickups.get(j).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(j).getDeliveryLocation().getCoordinate().getX()), 
    						new GeoPoint(pickups.get(i).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(i).getDeliveryLocation().getCoordinate().getX()));
    				travelTime = routingConnector.getTravelTime(
    						new GeoPoint(pickups.get(j).getDeliveryLocation().getCoordinate().getY(), 
    								pickups.get(j).getDeliveryLocation().getCoordinate().getX()), 
							new GeoPoint(pickups.get(i).getDeliveryLocation().getCoordinate().getY(), 
									pickups.get(i).getDeliveryLocation().getCoordinate().getX())) / 1000;
            		costMatrixBuilder.addTransportDistance(pickups.get(j).getDeliveryLocation().getId(), 
            				pickups.get(i).getDeliveryLocation().getId(), travelDistance);
            		costMatrixBuilder.addTransportTime(pickups.get(j).getDeliveryLocation().getId(), 
            				pickups.get(i).getDeliveryLocation().getId(), travelTime);
    			} catch (Exception e) {
    				throw new RoutingNotFoundException("Routing calculation between " 
    							+ pickups.get(i).getId() 
    							+ " and "
    							+ pickups.get(j).getId()
    							+ " not possible!");
    			}
        		
        	}
        	
        }
    	
    	// establish pickup to delivery relation
        for(int i = 0; i < pickups.size(); i++) {
        	
    		double travelDistance = 0.0;
    		int travelTime = 0;
			try {
				travelDistance = routingConnector.getTravelDistance(
						new GeoPoint(pickups.get(i).getPickupLocation().getCoordinate().getY(), 
								pickups.get(i).getPickupLocation().getCoordinate().getX()), 
						new GeoPoint(pickups.get(i).getDeliveryLocation().getCoordinate().getY(), 
								pickups.get(i).getDeliveryLocation().getCoordinate().getX()));
				travelTime = routingConnector.getTravelTime(
						new GeoPoint(pickups.get(i).getPickupLocation().getCoordinate().getY(), 
								pickups.get(i).getPickupLocation().getCoordinate().getX()), 
						new GeoPoint(pickups.get(i).getDeliveryLocation().getCoordinate().getY(), 
								pickups.get(i).getDeliveryLocation().getCoordinate().getX())) / 1000;
			} catch (Exception e) {
				throw new RoutingNotFoundException("Routing calculation from " 
						+ startingPoint
						+ " to service "
						+ pickups.get(i).getId()
						+ " not possible!");
			}
        	
        	costMatrixBuilder.addTransportDistance(pickups.get(i).getPickupLocation().getId(), 
        			pickups.get(i).getDeliveryLocation().getId(), travelDistance);
        	costMatrixBuilder.addTransportTime(pickups.get(i).getPickupLocation().getId(), 
        			pickups.get(i).getDeliveryLocation().getId(), travelTime);
        	
        }
    	
        // define vehicle to vehicle relation
        for(int i = 0; i < vehicles.size(); i++) {
        	
        	for(int j = i + 1; j < vehicles.size(); j++) {
        		costMatrixBuilder.addTransportDistance(vehicles.get(i).getId(), 
        				vehicles.get(j).getId(), 0);
        		costMatrixBuilder.addTransportTime(vehicles.get(i).getId(), 
        				vehicles.get(j).getId(), 0);	
        	}
        	
        }
		
		VehicleRoutingTransportCosts costMatrix = costMatrixBuilder.build();
		
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
		vrpBuilder.setRoutingCost(costMatrix);
        vrpBuilder.addAllVehicles(vehicles);
        vrpBuilder.addAllJobs(pickups);
		
        VehicleRoutingProblem problem = vrpBuilder.build();

        VehicleRoutingAlgorithm algorithm = new SchrimpfFactory().createAlgorithm(problem);
        algorithm.setMaxIterations(5000);

        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
		
        Collection<VehicleRoute> routes = bestSolution.getRoutes();
        
//        SolutionPrinter.print(problem, bestSolution, SolutionPrinter.Print.VERBOSE);
      
		org.json.simple.JSONObject obj = new org.json.simple.JSONObject();
		// create map with optimized tour
		Map<String, List<TransportProperties>> result = Maps.newHashMap();
        for(VehicleRoute route: routes) {
        	List<TransportProperties> jobList = Lists.newLinkedList();
        	for(TourActivity act : route.getActivities()) {
        		Job job = ((TourActivity.JobActivity) act).getJob();
				String tansportId = job.getId();
        		jobList.add(new TransportProperties(tansportId + "_" + act.getName(), 
        				Math.round(act.getArrTime()), 
        				Math.round(act.getEndTime()),
        				Math.round(act.getOperationTime())));
        	}
        	
        	result.put(route.getVehicle().getId(), jobList);
        }
		
        obj.putAll(result);
		return obj;
		
	}
	
	public org.json.simple.JSONObject startPlanningCare(JSONObject jsonObject) throws RoutingNotFoundException, InternalSchedulingErrorException {
		
		if(jsonObject == null 
				|| jsonObject.isNull("startLocation")
				|| jsonObject.isNull("vehicles")
				|| jsonObject.isNull("services")) {
			throw new InternalSchedulingErrorException("JSON is invalid, no scheduling possible for care scenario!");
		}
		
		// extract start location
		GeoPoint startFromCompany = null;
		if(jsonObject.has("startLocation")) {
			JSONObject location = jsonObject.getJSONObject("startLocation");
			double latitude = location.has("latitude") ? location.getDouble("latitude") : 0.0;
			double longitude = location.has("longitude") ? location.getDouble("longitude") : 0.0;
			startFromCompany = new GeoPoint(latitude, longitude);
		}
		
		List<JobConstraint> jobConstraints = null;
		if(jsonObject.has("constraints")) {
			jobConstraints = Lists.newLinkedList();
			JSONArray jsonArray = jsonObject.getJSONArray("constraints");
			for(int index = 0; index < jsonArray.length(); index++) {
				JSONObject constraintJSON = jsonArray.getJSONObject(index);
				String beforeJobId = constraintJSON.has("beforeJobId") ? constraintJSON.getString("beforeJobId") : "";
				String afterJobId = constraintJSON.has("afterJobId") ? constraintJSON.getString("afterJobId") : "";
				jobConstraints.add(new JobConstraint(beforeJobId, afterJobId));
			}
		}
		final List<JobConstraint> jobConstraintsFinal = jobConstraints;
		
		List<Vehicle> vehicles = null;
		if(jsonObject.has("vehicles")) {
			vehicles = Lists.newLinkedList();
			JSONArray jsonArray = jsonObject.getJSONArray("vehicles");
			for(int index = 0; index < jsonArray.length(); index++) {
				List<String> skills = Lists.newLinkedList();
				JSONObject vehicleJSON = jsonArray.getJSONObject(index);
				String vehicleID = vehicleJSON.has("vehicleID") ? vehicleJSON.getString("vehicleID") : "";
				int earliestStart = vehicleJSON.has("earliestStart") ? vehicleJSON.getInt("earliestStart") : 0;
				int latestArrival = vehicleJSON.has("latestArrival") ? vehicleJSON.getInt("latestArrival") : 0;
				int breakStartWindow = 0;
				int breakEndWindow = 0;
				int breakTime = 0;
				if(vehicleJSON.has("skills")) {
					JSONArray skillArray = vehicleJSON.getJSONArray("skills");
					for(int skillIndex = 0; skillIndex < skillArray.length(); skillIndex++) {
						skills.add(skillArray.getString(skillIndex));
					}
				}
				if(vehicleJSON.has("break")) {
					JSONObject breakJSON = vehicleJSON.getJSONObject("break");
					breakStartWindow = breakJSON.has("startWindow") ? breakJSON.getInt("startWindow") : 0;
					breakEndWindow = breakJSON.has("endWindow") ? breakJSON.getInt("endWindow") : 0;
					breakTime = breakJSON.has("breakTime") ? breakJSON.getInt("breakTime") : 0;
				}
				vehicles.add(new Vehicle(vehicleID, skills, earliestStart, 
						latestArrival, breakStartWindow, breakEndWindow, breakTime));
			}
		}
		
		List<Service> services = null;
		if(jsonObject.has("services")) {
			services = Lists.newLinkedList();
			JSONArray jsonArray = jsonObject.getJSONArray("services");
			for(int index = 0; index < jsonArray.length(); index++) {
				List<String> requiredSkills = Lists.newLinkedList();
				JSONObject serviceJSON = jsonArray.getJSONObject(index);
				String serviceID = serviceJSON.has("serviceID") ? serviceJSON.getString("serviceID") : "";
				int serviceTime = serviceJSON.has("serviceTime") ? serviceJSON.getInt("serviceTime") : 0;
				double longitude = 0.0;
				double latitude = 0.0;
				int start = 0;
				int end = 0;
				if(serviceJSON.has("requiredSkills")) {
					JSONArray skillArray = serviceJSON.getJSONArray("requiredSkills");
					for(int skillIndex = 0; skillIndex < skillArray.length(); skillIndex++) {
						requiredSkills.add(skillArray.getString(skillIndex));
					}
				}
				if(serviceJSON.has("location")) {
					JSONObject locationJSON = serviceJSON.getJSONObject("location");
					longitude = locationJSON.has("longitude") ? locationJSON.getDouble("longitude") : 0.0;
					latitude = locationJSON.has("latitude") ? locationJSON.getDouble("latitude") : 0.0;
				}
				if(serviceJSON.has("timeWindow")) {
					JSONObject timeJSON = serviceJSON.getJSONObject("timeWindow");
					start = timeJSON.has("start") ? timeJSON.getInt("start") : 0;
					end = timeJSON.has("end") ? timeJSON.getInt("end") : 0;
				}
				services.add(new Service(serviceID, new GeoPoint(latitude, longitude), serviceTime, 
						requiredSkills, start, end));
			}
		}

		VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix.Builder.newInstance(true);
		
		// calculate the same distance and time between the starting point and all patients
    	for(Service service: services) {
    		
    		double travelDistance = 0.0;
    		int travelTime = 0;
			try {
				travelDistance = routingConnector.getTravelDistance(startFromCompany, service.getLocation());
				travelTime = routingConnector.getTravelTime(startFromCompany, service.getLocation()) / 1000;
			} catch (Exception e) {
				throw new RoutingNotFoundException("Routing calculation from " 
						+ startFromCompany
						+ " to service "
						+ service.getServiceID()
						+ " not possible!");
			}

        	for(Vehicle vehicle: vehicles) {
        		costMatrixBuilder.addTransportDistance(vehicle.getVehicleID(), service.getServiceID(), travelDistance);
        		costMatrixBuilder.addTransportTime(vehicle.getVehicleID(), service.getServiceID(), travelTime);
        	}

    	}
    	
        for(int i = 0; i < services.size(); i++) {
        	
        	for(int j = i + 1; j < services.size(); j++) {
        		
        		double travelDistance = 0.0;
        		int travelTime = 0;
    			try {
    				travelDistance = routingConnector.getTravelDistance(services.get(i).getLocation(), services.get(j).getLocation());
    				travelTime = routingConnector.getTravelTime(services.get(i).getLocation(), services.get(j).getLocation()) / 1000;
    			} catch (Exception e) {
    				throw new RoutingNotFoundException("Routing calculation between " 
    							+ services.get(i).getServiceID() 
    							+ " and "
    							+ services.get(j).getServiceID()
    							+ " not possible!");
    			}
        		
        		costMatrixBuilder.addTransportDistance(services.get(i).getServiceID(), 
        				services.get(j).getServiceID(), travelDistance);
        		costMatrixBuilder.addTransportTime(services.get(i).getServiceID(), 
        				services.get(j).getServiceID(), travelTime);	
        	}
        	
        }
        
        for(int i = 0; i < vehicles.size(); i++) {
        	
        	for(int j = i + 1; j < vehicles.size(); j++) {
        		costMatrixBuilder.addTransportDistance(vehicles.get(i).getVehicleID(), 
        				vehicles.get(j).getVehicleID(), 0);
        		costMatrixBuilder.addTransportTime(vehicles.get(i).getVehicleID(), 
        				vehicles.get(j).getVehicleID(), 0);	
        	}
        	
        }
        
        VehicleRoutingTransportCosts costMatrix = costMatrixBuilder.build();

		VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("vehicleType");
		VehicleType vehicleType = vehicleTypeBuilder
				.setCostPerDistance(1)
				.build();
		
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
        vrpBuilder.setFleetSize(VehicleRoutingProblem.FleetSize.FINITE);
        vrpBuilder.setRoutingCost(costMatrix);
        
        for(Vehicle vehicle: vehicles) {
        	
    		Break lunchBreak = Break.Builder.newInstance("Pause " + vehicle.getVehicleID())
    				.setTimeWindow(TimeWindow.newInstance(vehicle.getBreakStartWindow(),
    						vehicle.getBreakEndWindow()))
    				.setServiceTime(vehicle.getBreakTime())
    				.setPriority(1)
    				.build();
            Builder vehicleBuilder = Builder.newInstance(vehicle.getVehicleID());
            vehicleBuilder.setStartLocation(Location.newInstance(vehicle.getVehicleID()));
            vehicleBuilder.setEndLocation(Location.newInstance(vehicle.getVehicleID()));
            vehicleBuilder.setType(vehicleType);
            for(String skill: vehicle.getSkills()) {
            	vehicleBuilder.addSkill(skill);
            }
            vehicleBuilder.setReturnToDepot(true);
            vehicleBuilder.setBreak(lunchBreak);
            vehicleBuilder.setLatestArrival(vehicle.getLatestArrival());
            vehicleBuilder.setEarliestStart(vehicle.getEarliestStart());
        	vrpBuilder.addVehicle(vehicleBuilder.build());
        	
        }
        
        for(Service service: services) {
        	
        	 com.graphhopper.jsprit.core.problem.job.Service.Builder serviceInstance = 
        			 com.graphhopper.jsprit.core.problem.job.Service.Builder.newInstance(service.getServiceID());
        	 		serviceInstance.setLocation(Location.newInstance(service.getServiceID()));
        	 		if(!service.getRequiredSkills().isEmpty()) {
        	 			for(String skill: service.getRequiredSkills()) {
        	 				serviceInstance.addRequiredSkill(skill);
        	 			}
        	 		}
            		serviceInstance.addTimeWindow(service.getStartWindow(),service.getEndWindow());
            		serviceInstance.setServiceTime(service.getServiceTime());
            		serviceInstance.setName(service.getServiceID());
  
        	vrpBuilder.addJob(serviceInstance.build());
        	
        }

        VehicleRoutingProblem problem = vrpBuilder.build();
        
        // add constraints for the routing problem
        StateManager stateManager = new StateManager(problem);
        ConstraintManager constraintManager = new ConstraintManager(problem, stateManager);
        
		HardActivityConstraint hardActivityConstraint = new HardActivityConstraint() {
			
			@Override
			public ConstraintsStatus fulfilled(JobInsertionContext iFacts,
					TourActivity prevAct, TourActivity newAct,
					TourActivity nextAct, double prevActDepTime) {
				
				if(jobConstraintsFinal != null && jobConstraintsFinal.size() > 0) {
					
					List<TourActivity> activities = iFacts.getRoute().getActivities();
					
					for(int const_index = 0; const_index < jobConstraintsFinal.size(); const_index++) {
						
						String idBefore = jobConstraintsFinal.get(const_index).getBeforeJobId();
						String idAfter = jobConstraintsFinal.get(const_index).getAfterJobId();
						int beforeIndex = 0;
						int afterIndex = 0;
						boolean foundBeforeAct = false;
						boolean foundAfterAct = false;
						
						for(int act_index = 0; act_index < activities.size(); act_index++) {
							
							TourActivity tourActivity = activities.get(act_index);
							
							if(tourActivity.getLocation().getId().equals(idBefore) 
									&& tourActivity.getName().equals("service")) {
								beforeIndex = act_index;
								foundBeforeAct = true;
							}
							if(tourActivity.getLocation().getId().equals(idAfter)
									&& tourActivity.getName().equals("service")) {
								afterIndex = act_index;
								foundAfterAct = true;
							}
							
						}
						
						if(foundAfterAct && foundBeforeAct 
								&& beforeIndex < afterIndex
								&& otherConstraintsDoNotFail(activities, const_index, jobConstraintsFinal)) {
							return ConstraintsStatus.FULFILLED;
						}
						if(foundAfterAct && foundBeforeAct && beforeIndex > afterIndex) {
							return ConstraintsStatus.NOT_FULFILLED;
						}
						
					}
					
				}
				
				return ConstraintsStatus.FULFILLED;
				
			}

			private boolean otherConstraintsDoNotFail(List<TourActivity> activities, 
					int index, List<JobConstraint> jobConstraintsFinal) {
				
				if(index >= jobConstraintsFinal.size()) {
					return true;
				} else {
					for(int i = index; i < jobConstraintsFinal.size(); i++) {
						
						String idBefore = jobConstraintsFinal.get(i).getBeforeJobId();
						String idAfter = jobConstraintsFinal.get(i).getAfterJobId();
						int beforeIndex = 0;
						int afterIndex = 0;
						boolean foundBeforeAct = false;
						boolean foundAfterAct = false;
						
						for(int act_index = 0; act_index < activities.size(); act_index++) {
							
							TourActivity tourActivity = activities.get(act_index);
							
							if(tourActivity.getLocation().getId().equals(idBefore) 
									&& tourActivity.getName().equals("service")) {
								beforeIndex = act_index;
								foundBeforeAct = true;
							}
							if(tourActivity.getLocation().getId().equals(idAfter)
									&& tourActivity.getName().equals("service")) {
								afterIndex = act_index;
								foundAfterAct = true;
							}
							
						}
						
						if(foundAfterAct && foundBeforeAct && beforeIndex > afterIndex) {
							return false;
						}
						
					}
				}
				
				return true;
			}

		}; 
		
		constraintManager.addConstraint(hardActivityConstraint, Priority.CRITICAL);

		/*
         * get the algorithm out-of-the-box.
		 */
		
		VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
	            .setStateAndConstraintManager(stateManager,constraintManager)
	            .addCoreStateAndConstraintStuff(true)
	            .buildAlgorithm();

		/*
         * and search a solution
		 */
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

		/*
         * get the best
		 */
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);
        Collection<VehicleRoute> routes = bestSolution.getRoutes();
        
//        SolutionPrinter.print(problem, bestSolution, SolutionPrinter.Print.VERBOSE);
        
		org.json.simple.JSONObject obj = new org.json.simple.JSONObject();
		// create map with optimized tour
		Map<String, List<JobProperties>> result = Maps.newHashMap();
        for(VehicleRoute route: routes) {
        	List<JobProperties> jobList = Lists.newLinkedList();
        	for(TourActivity act : route.getActivities()) {
        		Job job = ((TourActivity.JobActivity) act).getJob();
				String jobId = job.getId();
        		jobList.add(new JobProperties(jobId, 
        				Math.round(act.getArrTime()), 
        				Math.round(act.getEndTime()),
        				Math.round(act.getOperationTime())));
        	}
        	
        	result.put(route.getVehicle().getId(), jobList);
        }
		
        obj.putAll(result);
		return obj;
		
	}

	public List<PlanningResponse> startPlanningNew(JSONArray jsonArray) throws RoutingNotFoundException {
		
		List<PlanningResponse> planningList = Lists.newLinkedList();
		
		for(int index = 0; index < jsonArray.length(); index++) {
			
			JSONObject object = jsonArray.getJSONObject(index);
			if(object.has("StartLat") && object.has("StartLon") && object.has("EndLat") 
					&& object.has("EndLon") && object.has("startTimestamp") && object.has("endTimestamp")) {
				double startLatitude = object.getDouble("StartLat");
				double startLongitude = object.getDouble("StartLon");
				double endLatitude = object.getDouble("EndLat");
				double endLongitude = object.getDouble("EndLon");
				long startTimestamp = object.getLong("startTimestamp");
				long endTimestamp = object.getLong("endTimestamp");
				Date startDate = new Date(startTimestamp);
				Date endDate = new Date(endTimestamp);
				
				GeoPoint start = new GeoPoint(startLatitude, startLongitude);
				GeoPoint end = new GeoPoint(endLatitude, endLongitude);
				try {
					int travelTime = routingConnector.getTravelTime(start, end);
					double travelDistance = routingConnector.getTravelDistance(start, end);
					planningList.add(new PlanningResponse(travelTime, travelDistance, startDate, endDate, "1"));
				} catch (Exception e) {
					throw new RoutingNotFoundException("Routing problem happens");
				}
				
			}
			
		}
		
		Collections.sort(planningList);
		return planningList;
	}

}
 