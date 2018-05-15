package scheduling.component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.SchrimpfFactory;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Break;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl.Builder;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;

import beans.GeoPoint;
import beans.TransportProperties;
import exceptions.InternalSchedulingErrorException;
import exceptions.RoutingNotFoundException;
import rest.RoutingConnector;

@Component
public class PickupPlanner {
	
	private final Logger log;
	private final RoutingConnector routingConnector;

	public PickupPlanner() {
	    this.log = LoggerFactory.getLogger(this.getClass());
		this.routingConnector = new RoutingConnector();
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

}
 