package calendarextraction;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import beans.CalendarAppointment;
import beans.WorkingDay;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class AppointmentExtraction {
	
	private List<String> weekdays = Lists.newArrayList("monday", "tuesday", "wednesday", 
			"thursday", "friday", "saturday", "sunday");

	public List<String> extractCalendarUsers(JSONArray json) {
		
		List<String> users = Lists.newArrayList();
		
		for(int index = 0; index < json.length(); index++) {
			JSONObject object = json.getJSONObject(index);
			users.add(object.getString("name"));
		}
		return users;
		
	}
	
	List<CalendarAppointment> extractAppointments(JSONArray json) throws ParseException {
		
		List<CalendarAppointment> appointments = Lists.newArrayList();
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.GERMAN);
		
		for(int index = 0; index < json.length(); index++) {
			JSONObject object = json.getJSONObject(index);
			String id = object.getString("appointmentID");
			JSONObject jsonAppointment = object.getJSONObject("appointment");
			String begin = jsonAppointment.getString("begin");
			String end = jsonAppointment.getString("end");
			Date startDate = format.parse(begin);
			Date endDate = format.parse(end);
			CalendarAppointment appointment = new CalendarAppointment(null, startDate, endDate, id);
			appointments.add(appointment);
		}
		
		return appointments;
		
	}
	
	public Map<String, WorkingDay> extractWorkingHours(JSONObject json) {
		
		Map<String, WorkingDay> workingHours = Maps.newHashMap();
		
		for(String weekday: weekdays) {
			if(json.has(weekday)) {
				// extract break and working hours
				JSONObject object = json.getJSONObject(weekday);
				WorkingDay hours = extractHours(object);
				if(hours != null) {
					workingHours.put(weekday, hours);
				}
			}
		}
		
		return workingHours;
		
	}

	private WorkingDay extractHours(JSONObject object) {
		
		if(object.has("breakHours") && object.has("workingHours")) {
			// extract both values and generate dates
			JSONObject breakhours = object.getJSONObject("breakHours");
			JSONObject workinghours = object.getJSONObject("workingHours");
			
			int startBreakHour = breakhours.getJSONObject("start").getInt("hours");
			int startBreakMinute = breakhours.getJSONObject("start").getInt("minutes");
			int endBreakHour = breakhours.getJSONObject("end").getInt("hours");
			int endBreakMinute = breakhours.getJSONObject("end").getInt("minutes");
			int startWorkingHour = workinghours.getJSONObject("start").getInt("hours");
			int startWorkingMinute = workinghours.getJSONObject("start").getInt("minutes");
			int endWorkingHour = workinghours.getJSONObject("end").getInt("hours");
			int endWorkingMinute = workinghours.getJSONObject("end").getInt("minutes");
			
			return new WorkingDay(startBreakHour, startBreakMinute, endBreakHour, 
					endBreakMinute, startWorkingHour, startWorkingMinute, endWorkingHour, endWorkingMinute);	
		} else {
			return null;
		}
		
	}

}
