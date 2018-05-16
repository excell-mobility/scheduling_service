package scheduling.component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager;
import com.graphhopper.jsprit.core.problem.constraint.ConstraintManager.Priority;
import com.graphhopper.jsprit.core.problem.constraint.HardActivityConstraint;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Break;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

import beans.BlacklistItem;
import beans.GeoPoint;
import beans.JobConstraint;
import beans.JobProperties;
import beans.Service;
import beans.Vehicle;
import exceptions.InternalSchedulingErrorException;
import exceptions.RoutingNotFoundException;
import rest.RoutingConnector;

@Component
public class NursePlanner {
	
	private final Logger log;
	private final RoutingConnector routingConnector;

	public NursePlanner() {
	    this.log = LoggerFactory.getLogger(this.getClass());
		this.routingConnector = new RoutingConnector();
	}
	
	public org.json.simple.JSONObject startPlanningCare(JSONObject jsonObject) throws RoutingNotFoundException, InternalSchedulingErrorException {
		
		if(jsonObject == null 
				|| jsonObject.isNull("startLocation")
				|| jsonObject.isNull("vehicles")
				|| jsonObject.isNull("services")) {
			throw new InternalSchedulingErrorException("JSON input is invalid, no scheduling possible for care scenario!");
		}
		
		// extract start location
		GeoPoint startFromCompany = null;
		if(jsonObject.has("startLocation")) {
			JSONObject location = jsonObject.getJSONObject("startLocation");
			double latitude = location.has("latitude") ? location.getDouble("latitude") : 0.0;
			double longitude = location.has("longitude") ? location.getDouble("longitude") : 0.0;
			startFromCompany = new GeoPoint(latitude, longitude);
		}
		
		// extract job constraints
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
		
		Set<String> nurseIdSet = Sets.newHashSet();
		Set<String> patientIdSet = Sets.newHashSet();
		
		// extract vehicles
		List<Vehicle> vehicles = null;
		if(jsonObject.has("vehicles")) {
			vehicles = Lists.newLinkedList();
			JSONArray jsonArray = jsonObject.getJSONArray("vehicles");
			for(int index = 0; index < jsonArray.length(); index++) {
				List<String> skills = Lists.newLinkedList();
				JSONObject vehicleJSON = jsonArray.getJSONObject(index);
				String vehicleID = vehicleJSON.has("vehicleID") ? vehicleJSON.getString("vehicleID") : "";
				nurseIdSet.add(vehicleID);
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
		
		// extract services
		List<Service> services = Lists.newLinkedList();
		if(jsonObject.has("services")) {
			JSONArray jsonArray = jsonObject.getJSONArray("services");
			services.addAll(createServiceList(jsonArray, patientIdSet));
		}
		
		// extract blacklist
		List<BlacklistItem> blacklist = null;
		if(jsonObject.has("blacklist")) {
			blacklist = Lists.newLinkedList();
			JSONArray jsonArray = jsonObject.getJSONArray("blacklist");
			for(int index = 0; index < jsonArray.length(); index++) {
				JSONObject blacklistItemJSON = jsonArray.getJSONObject(index);
				String nurseId = blacklistItemJSON.has("nurseid") ? blacklistItemJSON.getString("nurseid") : "";
				String patientId = blacklistItemJSON.has("patientid") ? blacklistItemJSON.getString("patientid") : "";
				if(patientIdSet.contains(patientId) && nurseIdSet.contains(nurseId)) {
					blacklist.add(new BlacklistItem(nurseId, patientId));
				}
			}
		}
		
		// update services and vehicles
		if(blacklist != null && blacklist.size() > 0) {
			
			for(Service service: services) {
				for(BlacklistItem blacklistItem: blacklist) {
					if(service.getServiceID().equals(blacklistItem.getPatientId())) {
						List<String> requiredSkills = service.getRequiredSkills();
						requiredSkills = updateSkills(requiredSkills, blacklistItem.getPatientId());
						service.setRequiredSkills(requiredSkills);
					}
				}
			}
			
			for(Vehicle vehicle: vehicles) {
				for(BlacklistItem blacklistItem: blacklist) {
					if(!vehicle.getVehicleID().equals(blacklistItem.getNurseId())) {
						List<String> skills = vehicle.getSkills();
						skills = updateSkills(skills, blacklistItem.getPatientId());
						vehicle.setSkills(skills);
					}
				}
			}
			
		}

		// extract services already planned
		List<Service> plannedServices = Lists.newLinkedList();
		JSONObject currentPlanning = null;
        if(jsonObject.has("currentPlanning")) {
        	currentPlanning = jsonObject.getJSONObject("currentPlanning");
        	
    		for (Vehicle vehicle : vehicles) {
        	
    			if (currentPlanning.has(vehicle.getVehicleID())) {
    				
		        	JSONArray plannedJobs = currentPlanning.getJSONArray(vehicle.getVehicleID());
		        	plannedServices.addAll(createServiceList(plannedJobs, patientIdSet));
		        	
    			}
    		}
        }
		
		VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix.Builder.newInstance(true);
		
		// calculate the same distance and time between the starting point and all patients
		List<Service> allServices = Lists.newLinkedList();
		allServices.addAll(services);
		allServices.addAll(plannedServices);
    	for(Service service: allServices) {
    		
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
    	
        for(int i = 0; i < allServices.size(); i++) {
        	
        	for(int j = i + 1; j < allServices.size(); j++) {
        		
        		double travelDistance = 0.0;
        		int travelTime = 0;
    			try {
    				travelDistance = routingConnector.getTravelDistance(allServices.get(i).getLocation(), allServices.get(j).getLocation());
    				travelTime = routingConnector.getTravelTime(allServices.get(i).getLocation(), allServices.get(j).getLocation()) / 1000;
    			} catch (Exception e) {
    				throw new RoutingNotFoundException("Routing calculation between " 
    							+ allServices.get(i).getServiceID() 
    							+ " and "
    							+ allServices.get(j).getServiceID()
    							+ " not possible!");
    			}
        		
        		costMatrixBuilder.addTransportDistance(allServices.get(i).getServiceID(), 
        				allServices.get(j).getServiceID(), travelDistance);
        		costMatrixBuilder.addTransportTime(allServices.get(i).getServiceID(), 
        				allServices.get(j).getServiceID(), travelTime);	
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
        
        // prepare vehicles for VRP
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
        
        // prepare new services for VRP
        for(Service service: services)
        	vrpBuilder.addJob(buildService(service));

        // prepare initial services for VRP if exist
        if (currentPlanning != null) {
			for (com.graphhopper.jsprit.core.problem.vehicle.Vehicle vehicle : vrpBuilder.getAddedVehicles()) {
				if (currentPlanning.has(vehicle.getId())) {
	    			VehicleRoute.Builder initialRoute = VehicleRoute.Builder.newInstance(vehicle);
	
		        	for (Service plannedService : plannedServices) {
		        		com.graphhopper.jsprit.core.problem.job.Service service = buildService(plannedService);
		        		vrpBuilder.addJob(service);
		                initialRoute.addService(service);		                
		            }
		        	
		        	vrpBuilder.addInitialVehicleRoute(initialRoute.build());
				}
	        }
        }
    	
		// create new VRP
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
        				Math.round(act.getOperationTime())
        				));
        	}
        	
        	result.put(route.getVehicle().getId(), jobList);
        }
		
        obj.putAll(result);
		return obj;
		
	}

	private List<Service> createServiceList(JSONArray jsonArray, Set<String> patientIdSet) {
		List<Service> services = Lists.newLinkedList();
		for(int index = 0; index < jsonArray.length(); index++) {
			List<String> requiredSkills = Lists.newLinkedList();
			JSONObject serviceJSON = jsonArray.getJSONObject(index);

			String serviceID = null;
			if (serviceJSON.has("serviceID"))
				serviceID = serviceJSON.getString("serviceID");
			else {
				if (serviceJSON.has("jobID"))
					serviceID = serviceJSON.getString("jobID");
				else
					serviceID = "";
			}
			
			patientIdSet.add(serviceID);
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
			else {
				start = serviceJSON.has("startJob") ? serviceJSON.getInt("startJob") : 0;
				end = serviceJSON.has("endJob") ? serviceJSON.getInt("endJob") : 0;
				serviceTime = end - start;
			}
			services.add(new Service(serviceID, new GeoPoint(latitude, longitude), serviceTime, 
					requiredSkills, start, end));
		}		
		
		return services;
	}

	private List<String> updateSkills(List<String> requiredSkills,
			String patientId) {

		boolean found = false;
		for(String skill: requiredSkills) {
			if(skill.equals(patientId)) {
				found = true;
			}
		}
		if(!found) {
			requiredSkills.add(patientId);
		}
		
		return requiredSkills;
	}

	private com.graphhopper.jsprit.core.problem.job.Service buildService(Service service) {
	
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
    	
		return serviceInstance.build();
	}

}
 