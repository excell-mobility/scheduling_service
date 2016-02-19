package planningapi;

import org.json.simple.JSONArray;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import appointmentplanning.AppointmentPlanner;

@RestController
public class PlanningController {	
	
    @RequestMapping("/appointmentplanning")
    public JSONArray routing(@RequestParam(value="year", defaultValue="2015") Integer year,
    		@RequestParam(value="month", defaultValue="1") Integer month,
    		@RequestParam(value="day", defaultValue="1") Integer day,
    		@RequestParam(value="durationInMin", defaultValue="60") Integer durationOfAppointmentInMin,
    		@RequestParam(value="appointmentLat", defaultValue="0.0") Double appointmentLat,
    		@RequestParam(value="appointmentLon", defaultValue="0.0") Double appointmentLon) {
        return new AppointmentPlanner().startPlanning(year, month, day, 
        		durationOfAppointmentInMin, appointmentLat, appointmentLon);
    }
    
    @ExceptionHandler(value = Exception.class)
    public String inputParameterError() {
      return "Your input parameters for the appointment planning service are invalid!";
    }
    
}