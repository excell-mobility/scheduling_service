package utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class PropertyReader {
	
	private Properties properties;
	
	public PropertyReader() {
		properties = new Properties();
		extractProperties();
	}
	
	private void extractProperties() {
		
		FileInputStream inStream;
		try {
			inStream = new FileInputStream(new File("src/main/resources/application.properties"));
			properties.load(inStream);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		properties.getProperty("rest.routingendpoint");
		properties.getProperty("rest.calendarendpoint");
		
	}
	
	public String getPropertyValue(String key) {
		
		return properties.getProperty(key);
		
	}

}
