package utility;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestMeasureConverter {

	@Test
	public void testGetTimeInMinutes() {
		
		assertEquals(3, MeasureConverter.getTimeInMinutes(154945));
		assertEquals(3, MeasureConverter.getTimeInMinutes(180000));
		assertEquals(2, MeasureConverter.getTimeInMinutes(110000));
		assertEquals(1, MeasureConverter.getTimeInMinutes(50000));
		assertEquals(0, MeasureConverter.getTimeInMinutes(0));
		assertEquals(1, MeasureConverter.getTimeInMinutes(2000));
		
	}

}
