package scheduling.component;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

import beans.GeoPoint;
import exceptions.RoutingNotFoundException;
import rest.RoutingConnector;
import scheduling.model.PlanningResponse;

@Component
public class TravelTimeSorter {
	
	private final RoutingConnector routingConnector;

	public TravelTimeSorter() {
		this.routingConnector = new RoutingConnector();
	}
	
	public List<PlanningResponse> startPlanning(JSONArray jsonArray) throws RoutingNotFoundException {
		
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
 