// package scheduling;

// import static org.junit.Assert.assertFalse;
// import static org.junit.Assert.assertTrue;

// import java.util.List;

// import org.junit.Before;
// import org.junit.Test;

// import scheduling.exceptions.InternalSchedulingErrorException;
// //import scheduling.component.AppointmentPlanner;
// import scheduling.model.PlanningResponse;

// public class TestAppointmentPlanner {
	
// 	private AppointmentPlanner planner;
	
// 	@Before
// 	public void createDatastructures() {
// 		planner = new AppointmentPlanner();
// 	}

// 	@Test
// 	public void testAppointmentPlanningNotPossible() throws InternalSchedulingErrorException {
		
// 		List<PlanningResponse> startPlanning = planner.startPlanning(2016, 02, 24, 1000, 51.030306, 13.730407);
// 		System.out.println(startPlanning);
// 		assertTrue(startPlanning.isEmpty());
// 	}
	
// 	@Test
// 	public void testAppointmentPlanningBeginning() throws InternalSchedulingErrorException {

// 		List<PlanningResponse> startPlanning = planner.startPlanning(2016, 02, 24, 50, 51.030306, 13.730407);
// 		assertFalse(startPlanning.get(0).toString().contains("Error"));
// 		assertFalse(startPlanning.isEmpty());
// 	}
	
// 	@Test
// 	public void testAppointmentPlanningEnd() throws InternalSchedulingErrorException {
		
// 		List<PlanningResponse> startPlanning = planner.startPlanning(2016, 02, 24, 58, 51.030306, 13.730407);
// 		assertFalse(startPlanning.get(0).toString().contains("Error"));
		
// 	}

// }
