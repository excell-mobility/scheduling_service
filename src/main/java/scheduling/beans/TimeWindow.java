package scheduling.beans;

import io.swagger.annotations.ApiModelProperty;

public class TimeWindow {
	
	@ApiModelProperty(notes = "Start time in millis where midnight = 0", required = true)
	private int start;
	@ApiModelProperty(notes = "End time in millis where midnight = 0", required = true)
	private int end;
	
	public TimeWindow(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getEnd() {
		return end;
	}
	
	public void setEnd(int end) {
		this.end = end;
	}
	
	@Override
	public String toString() {
		return "TimeWindow [ start = " + start + ", end =" + end + "]";
	}

}
