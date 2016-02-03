package appointmentplanning;

import static org.junit.Assert.*;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class TestAppointmentPlanner {
	
	private AppointmentPlanner planner;
	
	@Before
	public void createDatastructures() {	
		planner = new AppointmentPlanner();
	}

	@Test
	public void testAppointmentPlanningNotPossible() {
		
		JSONObject startPlanning = planner.startPlanning(2015, 11, 10, 120, 51.030306, 13.730407);
		assertTrue(startPlanning.containsKey("Error"));
		
	}
	
	@Test
	public void testAppointmentPlanningBeginning() {
		
		JSONObject startPlanning = planner.startPlanning(2015, 11, 10, 50, 51.030306, 13.730407);
		assertFalse(startPlanning.containsKey("Error"));
		
	}
	
	@Test
	public void testAppointmentPlanningEnd() {
		
		JSONObject startPlanning = planner.startPlanning(2015, 11, 10, 58, 51.030306, 13.730407);
		assertFalse(startPlanning.containsKey("Error"));
		
	}

}
