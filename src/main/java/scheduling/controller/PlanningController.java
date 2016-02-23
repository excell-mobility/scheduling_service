package scheduling.controller;

import org.json.simple.JSONArray;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import scheduling.component.AppointmentPlanner;
import beans.Timeslot;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RestController
@Api(value="/v1/scheduling")
public class PlanningController {	
	
	@RequestMapping(value = "/v1/scheduling", method = RequestMethod.GET)
    @ApiOperation(
    		value = "Get possible time slots for appointments", 
    		response=Timeslot.class, 
    		responseContainer="List",
    		produces = "application/json")
    @ResponseBody
    public JSONArray scheduling(
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
    		@RequestParam(value="appointmentLon", defaultValue="0.0") Double appointmentLon) {
        
		return new AppointmentPlanner().startPlanning(year, month, day, 
        		durationOfAppointmentInMin, appointmentLat, appointmentLon);
    }
    
    @ExceptionHandler(value = Exception.class)
    public String inputParameterError() {
      return "Your input parameters for the appointment planning service are invalid!";
    }
    
}