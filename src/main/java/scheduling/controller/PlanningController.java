package scheduling.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import scheduling.component.AppointmentPlanner;
import scheduling.model.PlanningResponse;

@CrossOrigin(origins = "*")
@RestController
@Api(value="/v1/scheduling")
public class PlanningController {
	
	@Autowired
	private AppointmentPlanner appointmentPlanner;
	
	@RequestMapping(value = "/v1/scheduling", method = RequestMethod.GET)
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
    		@RequestParam(value="appointmentLon", defaultValue="0.0") Double appointmentLon) {
        
		return appointmentPlanner.startPlanning(year, month, day, 
        		durationOfAppointmentInMin, appointmentLat, appointmentLon);
    }
    
    @ExceptionHandler(value = Exception.class)
    public String inputParameterError() {
      return "Your input parameters for the appointment planning service are invalid!";
    }
    
}