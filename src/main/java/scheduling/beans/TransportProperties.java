package scheduling.beans;

import java.io.Serializable;

public class TransportProperties implements Serializable {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -8916079353276283351L;
	private String transportID;
	private long startJob;
	private long endJob;
	private long serviceTime;
	
	public TransportProperties(String transportID, long startJob, long endJob, long serviceTime) {
		this.transportID = transportID;
		this.startJob = startJob;
		this.endJob = endJob;
		this.serviceTime = serviceTime;
	}

	public String getTransportID() {
		return transportID;
	}

	public void setTransportID(String transportID) {
		this.transportID = transportID;
	}

	public long getStartJob() {
		return startJob;
	}

	public void setStartJob(long startJob) {
		this.startJob = startJob;
	}

	public long getEndJob() {
		return endJob;
	}

	public void setEndJob(long endJob) {
		this.endJob = endJob;
	}

	public long getServiceTime() {
		return serviceTime;
	}

	public void setServiceTime(long serviceTime) {
		this.serviceTime = serviceTime;
	}

	@Override
	public String toString() {
		return "TransportProperties [transportID=" + transportID
				+ ", startJob=" + startJob + ", endJob=" + endJob
				+ ", serviceTime=" + serviceTime + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (endJob ^ (endJob >>> 32));
		result = prime * result + (int) (serviceTime ^ (serviceTime >>> 32));
		result = prime * result + (int) (startJob ^ (startJob >>> 32));
		result = prime * result
				+ ((transportID == null) ? 0 : transportID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TransportProperties other = (TransportProperties) obj;
		if (endJob != other.endJob)
			return false;
		if (serviceTime != other.serviceTime)
			return false;
		if (startJob != other.startJob)
			return false;
		if (transportID == null) {
			if (other.transportID != null)
				return false;
		} else if (!transportID.equals(other.transportID))
			return false;
		return true;
	}
	
}
