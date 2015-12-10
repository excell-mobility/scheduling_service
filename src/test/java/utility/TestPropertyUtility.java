package utility;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class TestPropertyUtility {

	@Test
	public void testPropertyHelper() throws IOException {

		PropertyReader reader = new PropertyReader();
		assertFalse(reader.getPropertyValue("rest.routingendpoint").isEmpty());
		assertFalse(reader.getPropertyValue("rest.calendarendpoint").isEmpty());
		
	}

}
