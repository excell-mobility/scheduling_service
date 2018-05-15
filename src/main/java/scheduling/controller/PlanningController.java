package scheduling.controller;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import exceptions.InternalSchedulingErrorException;
import exceptions.RoutingNotFoundException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import scheduling.component.AppointmentPlanner;
import scheduling.component.NursePlanner;
import scheduling.component.PickupPlanner;
import scheduling.component.TravelTimeSorter;
import scheduling.model.CareScenarioResponse;
import scheduling.model.PickupScenarioResponse;
import scheduling.model.PlanningResponse;

@SuppressWarnings("deprecation")
@CrossOrigin(origins = "*")
@RestController
@Api(value="/v1/scheduling")
public class PlanningController {
	
	@Autowired
	private TravelTimeSorter travelTimeSorter;
	private NursePlanner nursePlanner;
	private PickupPlanner pickupPlanner;
	private AppointmentPlanner appointmentPlanner;
	
	@RequestMapping(value = "/v1/schedulingnew", method = RequestMethod.POST)
    @ApiOperation(
    		value = "Get possible time slots for appointments", 
    		response=PlanningResponse.class, 
    		responseContainer="List",
    		produces = "application/json")
    @ResponseBody
    public List<PlanningResponse> schedulingnew(
    		@ApiParam(name="jsonArrayInput", value="JSON array of time gaps with coordinates")
    		@RequestBody String jsonArrayInput) throws RoutingNotFoundException {
    		JSONArray jsonArray = new JSONArray(jsonArrayInput);
    		return travelTimeSorter.startPlanning(jsonArray);
    }
	
	@RequestMapping(value = "/v1/schedulingcare", method = RequestMethod.POST)
    @ApiOperation(
    		value = "Generate the scheduling for elder care", 
    		response=CareScenarioResponse.class, 
    		responseContainer="Map",
    		produces = "application/json")
    @ResponseBody
    public org.json.simple.JSONObject schedulingcare(
    		@ApiParam(name="jsonObjectInput", value="JSON object with patients and carers informationen")
    		@RequestBody String jsonObjectInput) throws RoutingNotFoundException, InternalSchedulingErrorException {
    		JSONObject jsonObject = new JSONObject(jsonObjectInput);
    		return nursePlanner.startPlanningCare(jsonObject);
    }
	
	@RequestMapping(value = "/v1/schedulingpickup", method = RequestMethod.POST)
    @ApiOperation(
    		value = "Generate the scheduling for pickup", 
    		response=PickupScenarioResponse.class, 
    		responseContainer="Map",
    		produces = "application/json")
    @ResponseBody
    public org.json.simple.JSONObject schedulingpickup(
    		@ApiParam(name="jsonObjectInput", value="JSON object with pickup and dropoff locations")
    		@RequestBody String jsonObjectInput) throws RoutingNotFoundException, InternalSchedulingErrorException {
    		JSONObject jsonObject = new JSONObject(jsonObjectInput);
    		return pickupPlanner.startPlanningPickup(jsonObject);
    }
	
	@RequestMapping(value = "/v1/scheduling", method = RequestMethod.GET)
	@Deprecated
    @ApiOperation(
    		value = "Get possible time slots for appointments", 
    		response=PlanningResponse.class, 
    		responseContainer="List",
    		produces = "application/json")
    @ResponseBody
    public List<PlanningResponse> scheduling(
    		@ApiParam(name="year", value="Year of new appointment", defaultValue="2016") 
    		@RequestParam(value="year", defaultValue="2015") Integer year,
    		
    		@ApiParam(name="month", value="Month of new appointment", defaultValue="1")
    		@RequestParam(value="month", defaultValue="1") Integer month,
    		
    		@ApiParam(name="day", value="Day of new appointment", defaultValue="1")
    		@RequestParam(value="day", defaultValue="1") Integer day,
    		
    		@ApiParam(name="durationInMin", value="Duration of new appointment", defaultValue="60")
    		@RequestParam(value="durationInMin", defaultValue="60") Integer durationOfAppointmentInMin,
    		
    		@ApiParam(name="appointmentLat", value="Latitude of new appointment", defaultValue="51.029")
    		@RequestParam(value="appointmentLat", defaultValue="0.0") Double appointmentLat,
    		
    		@ApiParam(name="appointmentLon", value="Longitude of new appointment", defaultValue="13.736") 
    		@RequestParam(value="appointmentLon", defaultValue="0.0") Double appointmentLon) throws InternalSchedulingErrorException {
        
		return appointmentPlanner.startPlanning(year, month, day, 
        		durationOfAppointmentInMin, appointmentLat, appointmentLon);
    }
    
    @ExceptionHandler(value = RoutingNotFoundException.class)
    public BodyBuilder routingError() {
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(value = InternalSchedulingErrorException.class)
    public BodyBuilder schedulingError() {
    	return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
}