package scheduling;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.simple.JSONArray;
import org.junit.Before;
import org.junit.Test;

import scheduling.component.AppointmentPlanner;

public class TestAppointmentPlanner {
	
	private AppointmentPlanner planner;
	
	@Before
	public void createDatastructures() {
		planner = new AppointmentPlanner();
	}

	@Test
	public void testAppointmentPlanningNotPossible() {
		
		JSONArray startPlanning = planner.startPlanning(2015, 11, 10, 120, 51.030306, 13.730407);
		assertTrue(startPlanning.get(0).toString().contains("Error"));
		
	}
	
	@Test
	public void testAppointmentPlanningBeginning() {

		JSONArray startPlanning = planner.startPlanning(2015, 11, 10, 50, 51.030306, 13.730407);
		assertFalse(startPlanning.get(0).toString().contains("Error"));
		
	}
	
	@Test
	public void testAppointmentPlanningEnd() {
		
		JSONArray startPlanning = planner.startPlanning(2015, 11, 10, 58, 51.030306, 13.730407);
		assertFalse(startPlanning.get(0).toString().contains("Error"));
		
	}

}
