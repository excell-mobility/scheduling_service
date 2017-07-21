package scheduling.component;

import java.io.IOException;
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Break;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivities;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

import beans.CalendarAppointment;
import beans.GeoPoint;
import beans.Service;
import beans.Vehicle;
//import beans.Timeslot;
import beans.WorkingDay;
import exceptions.RoutingNotFoundException;
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
	
	public org.json.simple.JSONObject startPlanningCare(JSONObject jsonObject) {
		
		// extract start location
		GeoPoint startFromCompany = null;
		if(jsonObject.has("startLocation")) {
			JSONObject location = jsonObject.getJSONObject("startLocation");
			double latitude = location.has("latitude") ? location.getDouble("latitude") : 0.0;
			double longitude = location.has("longitude") ? location.getDouble("longitude") : 0.0;
			startFromCompany = new GeoPoint(latitude, longitude);
		}
		
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
				log.error("Routing calculation not possible!");
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
    				log.error("Routing calculation not possible!");
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

		/*
         * get the algorithm out-of-the-box.
		 */

        VehicleRoutingAlgorithm algorithm = Jsprit.createAlgorithm(problem);

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
		Map<String, List<String>> result = Maps.newHashMap();
        for(VehicleRoute route: routes) {
        	List<String> jobList = Lists.newLinkedList();
        	for(TourActivity act : route.getActivities()) {
        		String jobId = ((TourActivity.JobActivity) act).getJob().getId();
        		jobList.add(jobId);
        	}
        	
        	result.put(route.getVehicle().getId(), jobList);
        }
		
        obj.putAll(result);
		return obj;
		
	}

	public List<PlanningResponse> startPlanningNew(JSONArray jsonArray) {
		
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
					log.error("Routing problem happens " + e);
				}
				
			}
			
		}
		
		Collections.sort(planningList);
		return planningList;
	}

}
 