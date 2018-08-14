package scheduling.beans;

public class WorkingDay {
	
	private int startBreakHour;
	private int startBreakMinute;
	private int endBreakHour;
	private int endBreakMinute;
	private int startWorkingHour;
	private int startWorkingMinute;
	private int endWorkingHour;
	private int endWorkingMinute;
	
	public WorkingDay(int startBreakHour, int startBreakMinute,
			int endBreakHour, int endBreakMinute, int startWorkingHour,
			int startWorkingMinute, int endWorkingHour, int endWorkingMinute) {
		this.startBreakHour = startBreakHour;
		this.startBreakMinute = startBreakMinute;
		this.endBreakHour = endBreakHour;
		this.endBreakMinute = endBreakMinute;
		this.startWorkingHour = startWorkingHour;
		this.startWorkingMinute = startWorkingMinute;
		this.endWorkingHour = endWorkingHour;
		this.endWorkingMinute = endWorkingMinute;
	}
	public int getStartBreakHour() {
		return startBreakHour;
	}
	public void setStartBreakHour(int startBreakHour) {
		this.startBreakHour = startBreakHour;
	}
	public int getStartBreakMinute() {
		return startBreakMinute;
	}
	public void setStartBreakMinute(int startBreakMinute) {
		this.startBreakMinute = startBreakMinute;
	}
	public int getEndBreakHour() {
		return endBreakHour;
	}
	public void setEndBreakHour(int endBreakHour) {
		this.endBreakHour = endBreakHour;
	}
	public int getEndBreakMinute() {
		return endBreakMinute;
	}
	public void setEndBreakMinute(int endBreakMinute) {
		this.endBreakMinute = endBreakMinute;
	}
	public int getStartWorkingHour() {
		return startWorkingHour;
	}
	public void setStartWorkingHour(int startWorkingHour) {
		this.startWorkingHour = startWorkingHour;
	}
	public int getStartWorkingMinute() {
		return startWorkingMinute;
	}
	public void setStartWorkingMinute(int startWorkingMinute) {
		this.startWorkingMinute = startWorkingMinute;
	}
	public int getEndWorkingHour() {
		return endWorkingHour;
	}
	public void setEndWorkingHour(int endWorkingHour) {
		this.endWorkingHour = endWorkingHour;
	}
	public int getEndWorkingMinute() {
		return endWorkingMinute;
	}
	public void setEndWorkingMinute(int endWorkingMinute) {
		this.endWorkingMinute = endWorkingMinute;
	}
	@Override
	public String toString() {
		return "WorkingDay [startBreakHour=" + startBreakHour
				+ ", startBreakMinute=" + startBreakMinute + ", endBreakHour="
				+ endBreakHour + ", endBreakMinute=" + endBreakMinute
				+ ", startWorkingHour=" + startWorkingHour
				+ ", startWorkingMinute=" + startWorkingMinute
				+ ", endWorkingHour=" + endWorkingHour + ", endWorkingMinute="
				+ endWorkingMinute + "]";
	}

}
