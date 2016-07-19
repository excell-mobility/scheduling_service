package optimizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

import beans.CalendarAppointment;
import beans.GeoPoint;
import beans.WorkingDay;
//import rest.RoutingConnector;
import scheduling.component.TourOptimizer;
import scheduling.model.PlanningResponse;

public class TestOptimizer {
	
	List<CalendarAppointment> appointments;
	WorkingDay workingDay;
	GeoPoint startPosition;
	TourOptimizer optimizer;
	
	@Before
	public void createDatastructures() {
		
		appointments = Lists.newArrayList();
		appointments.add(new CalendarAppointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), "1"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), new GregorianCalendar(2015, 11, 10, 13, 00).getTime(), "2"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 14, 00).getTime(), new GregorianCalendar(2015, 11, 10, 15, 00).getTime(), "3"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.038104, 13.775029),
				new GregorianCalendar(2015, 11, 10, 17, 00).getTime(), new GregorianCalendar(2015, 11, 10, 18, 00).getTime(), "4"));
		
		workingDay = new WorkingDay(11, 30, 12, 0, 8, 30, 18, 30);
		
		startPosition = new GeoPoint(51.030201,13.727380);
		
		Date beginningDate = new GregorianCalendar(2015, 11, 10, 
				workingDay.getStartWorkingHour(), workingDay.getStartWorkingMinute()).getTime();
		Date endDate = new GregorianCalendar(2015, 11, 10, 
				workingDay.getEndWorkingHour(), workingDay.getEndWorkingMinute()).getTime();
		Date beginningBreak = new GregorianCalendar(2015, 11, 10, 
				workingDay.getStartBreakHour(), workingDay.getStartBreakMinute()).getTime();
		Date endBreak = new GregorianCalendar(2015, 11, 10, 
				workingDay.getEndBreakHour(), workingDay.getEndBreakMinute()).getTime();
		
		optimizer = new TourOptimizer(
				appointments,
				beginningDate,
				endDate,
				beginningBreak,
				endBreak,
				startPosition,
				startPosition,
				"test"
				);
	}
	
	@Test
	public void testAppointmentOptimization() throws Exception {
		
		//optimizer.setBeginBreak(new GregorianCalendar(2015, 11, 10, 12, 05).getTime());
		//optimizer.setEndBreak(new GregorianCalendar(2015, 11, 10, 13, 05).getTime());
		List<PlanningResponse> possibleTimeslotForNewAppointment = optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 30);
		assertEquals(new GregorianCalendar(2015, 11, 10, 10, 05).getTime(), 
				possibleTimeslotForNewAppointment.get(0).getStartDate());
		assertEquals(new GregorianCalendar(2015, 11, 10, 11, 54).getTime(), 
				possibleTimeslotForNewAppointment.get(0).getEndDate());
		assertEquals(new GregorianCalendar(2015, 11, 10, 15, 05).getTime(), 
				possibleTimeslotForNewAppointment.get(1).getStartDate());
		assertEquals(new GregorianCalendar(2015, 11, 10, 16, 53).getTime(), 
				possibleTimeslotForNewAppointment.get(1).getEndDate());
		assertTrue(possibleTimeslotForNewAppointment.get(0).getTravelDistance() > 0.0);
		
		possibleTimeslotForNewAppointment = 
				optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 109);
		assertTrue(possibleTimeslotForNewAppointment.isEmpty());
		
	}

	
	@Test
	public void testCheckTimeslotForNewAppointment() {
		
		appointments.remove(0);
		appointments.remove(0);
		CalendarAppointment appointment120 = new CalendarAppointment(
				new GeoPoint(51.030306, 13.730407), 
				new GregorianCalendar(2015, 11, 10, 15, 00).getTime(), 
				new GregorianCalendar(2015, 11, 10, 17, 00).getTime(), "does_not_fit_in");
		assertFalse(optimizer.checkTimeslotForNewAppointment(appointment120));
		
		CalendarAppointment appointment60 = new CalendarAppointment(
				new GeoPoint(51.030306, 13.730407), 
				new GregorianCalendar(2015, 11, 10, 15, 30).getTime(), 
				new GregorianCalendar(2015, 11, 10, 16, 30).getTime(), "does_fit_in");
		assertTrue(optimizer.checkTimeslotForNewAppointment(appointment60));
		
	}
	
	@Test
	public void testNullAppointmentOptimization() throws Exception {		
		
		appointments = Lists.newArrayList();
		appointments.add(new CalendarAppointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), "1"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), "2"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 12, 30).getTime(), new GregorianCalendar(2015, 11, 10, 18, 00).getTime(), "3"));
		
		optimizer.setAppointments(appointments);
		assertTrue(optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 30).isEmpty());
		
		appointments = Lists.newArrayList();
		appointments.add(new CalendarAppointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), "1"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), "2"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 12, 40).getTime(), new GregorianCalendar(2015, 11, 10, 15, 00).getTime(), "3"));
		
		optimizer.setAppointments(appointments);
		assertTrue(!optimizer.getPossibleTimeslotForNewAppointment(new GeoPoint(51.030306, 13.730407), 30).isEmpty());
		
	}
	
	/*
	@Test
	public void getBestRoute() throws Exception {
		
		RoutingConnector router = new RoutingConnector();
		TourOptimizer optimizer = new TourOptimizer(router);
		
		optimizer.getOptimalRoute(new GeoPoint[] {
				new GeoPoint(51.0199692, 13.7694339),
				new GeoPoint(51.0219061, 13.7329655),
				new GeoPoint(51.0212516, 13.7347557),
				new GeoPoint(51.0257707, 13.7490612),
				new GeoPoint(51.0271202, 13.7501205),
				new GeoPoint(51.0270662, 13.7503522),
				new GeoPoint(51.0238042, 13.7519395660526),
				new GeoPoint(51.0304768, 13.7683087)
		});
		
		List<Double[]> optimalRoute = optimizer.getOptimalRoute(new GeoPoint[] {
				new GeoPoint(51.057478, 13.7687908),
				new GeoPoint(51.0309365, 13.7837262),
				new GeoPoint(51.0300575, 13.7938012),
				new GeoPoint(51.0487785, 13.7669101),
				new GeoPoint(51.0456306, 13.7716843605751)
		});
		
		assertTrue(optimalRoute.size() == 50);
		
		optimizer.getOptimalRoute(new GeoPoint[] {
				new GeoPoint(51.0200927, 13.7636955),
				new GeoPoint(51.0110074, 13.7663787),
				new GeoPoint(51.0115947, 13.7600701),
				new GeoPoint(51.0219261, 13.7525135),
				new GeoPoint(51.0233212, 13.7509321),
				new GeoPoint(51.0208117, 13.751226)
		});

		optimizer.getOptimalRoute(new GeoPoint[] {
				new GeoPoint(51.0443775, 13.7365646),
				new GeoPoint(51.0304768, 13.7683087),
				new GeoPoint(51.0339175, 13.7578351),
				new GeoPoint(51.0341596, 13.7549376),
				new GeoPoint(51.0346703, 13.7561141),
				new GeoPoint(51.0274485, 13.7599948),
				new GeoPoint(51.0326294, 13.7606689),
				new GeoPoint(51.03690555, 13.750088596189)
		});

		optimizer.getOptimalRoute(new GeoPoint[] {
				new GeoPoint(51.0159072, 13.7845261),
				new GeoPoint(51.0167902, 13.7877536),
				new GeoPoint(51.0168634, 13.7887410655414),
				new GeoPoint(51.0071114, 13.8026268),
				new GeoPoint(51.0063267, 13.8012339),
				new GeoPoint(51.00734445, 13.7930717936427),
				new GeoPoint(51.0182285, 13.7733799),
				new GeoPoint(51.018089, 13.7737184),
				new GeoPoint(51.0168224, 13.7885229)
		});

		optimizer.getOptimalRoute(new GeoPoint[] {
				new GeoPoint(51.1246449, 13.8340082914946),
				new GeoPoint(51.0434033, 13.7390605),
				new GeoPoint(51.0453316, 13.7248058),
				new GeoPoint(51.0283256, 13.7565921),
				new GeoPoint(51.0264513, 13.7694191),
				new GeoPoint(51.0572192, 13.7685295)
		});

		optimizer.getOptimalRoute(new GeoPoint[] {
				new GeoPoint(51.01982695, 13.7580610420333),
				new GeoPoint(51.019165, 13.7572845),
				new GeoPoint(51.0256184, 13.7592234),
				new GeoPoint(51.0254617, 13.8031619),
				new GeoPoint(51.0234259, 13.8014061),
				new GeoPoint(51.0227336, 13.7431401)
		});
	}*/

}
