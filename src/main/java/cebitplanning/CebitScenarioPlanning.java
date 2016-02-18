package cebitplanning;

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TreeMap;

import org.json.JSONException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import beans.CalendarAppointment;
import beans.GeoPoint;
import optimizer.TourOptimizer;

public class CebitScenarioPlanning {

	public static void main(String[] args) throws JSONException, IOException {
		
		// tour optimizer for calculating travel times
		TourOptimizer optimizer =  new TourOptimizer(null);
		
		// create list with appointments
		List<CalendarAppointment> appointments = Lists.newLinkedList();
		appointments.add(new CalendarAppointment(new GeoPoint(51.042239, 13.731460),
				new GregorianCalendar(2015, 11, 10, 9, 00).getTime(), new GregorianCalendar(2015, 11, 10, 10, 00).getTime(), "1"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.057536, 13.741229),
				new GregorianCalendar(2015, 11, 10, 12, 00).getTime(), new GregorianCalendar(2015, 11, 10, 13, 00).getTime(), "2"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.052599, 13.752138),
				new GregorianCalendar(2015, 11, 10, 14, 00).getTime(), new GregorianCalendar(2015, 11, 10, 15, 00).getTime(), "3"));
		appointments.add(new CalendarAppointment(new GeoPoint(51.038104, 13.775029),
				new GregorianCalendar(2015, 11, 10, 17, 00).getTime(), new GregorianCalendar(2015, 11, 10, 18, 00).getTime(), "4"));
		
		// print out travel time of the first route
		System.out.println("Initial travel time in minutes: " + optimizer.calculateTravelTimes(appointments));
		
		// calculate permutations for the appointments
		List<List<CalendarAppointment>> permutedList = generatePerm(appointments);

		// calculate travel times for each permutation and save in tree map
		TreeMap<Integer, List<CalendarAppointment>> sortedTourByTraveltime = Maps.newTreeMap();
		for (List<CalendarAppointment> tour: permutedList) {
			sortedTourByTraveltime.put(optimizer.calculateTravelTimes(tour), 
					tour);
		}
		
		// print out the combination with the minimized travel time
		System.out.println("First: " + sortedTourByTraveltime.firstKey() + " min for " + sortedTourByTraveltime.get(sortedTourByTraveltime.firstKey()));
		System.out.println("Last: " + sortedTourByTraveltime.lastKey() + " min for " + sortedTourByTraveltime.get(sortedTourByTraveltime.lastKey()));
	
	}
	
	 private static List<List<CalendarAppointment>> generatePerm(List<CalendarAppointment> original) {
	     if (original.size() == 0) { 
	       List<List<CalendarAppointment>> result = new ArrayList<List<CalendarAppointment>>();
	       result.add(new ArrayList<CalendarAppointment>());
	       return result;
	     }
	     CalendarAppointment firstElement = original.remove(0);
	     List<List<CalendarAppointment>> returnValue = new ArrayList<List<CalendarAppointment>>();
	     List<List<CalendarAppointment>> permutations = generatePerm(original);
	     for (List<CalendarAppointment> smallerPermutated : permutations) {
	       for (int index=0; index <= smallerPermutated.size(); index++) {
	         List<CalendarAppointment> temp = new ArrayList<CalendarAppointment>(smallerPermutated);
	         temp.add(index, firstElement);
	         returnValue.add(temp);
	       }
	     }
	     return returnValue;
	   }

}
