package scheduling.beans;

import java.io.Serializable;

public class ServiceVehicleConstraint implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String serviceId;
	private String vehicleId;
	
	public ServiceVehicleConstraint(String serviceId, String vehicleId) {
		this.serviceId = serviceId;
		this.vehicleId = vehicleId;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}
	
	@Override
	public String toString() {
		return "ServiceVehicleConstraint [serviceId=" + serviceId + ", vehicleId=" + vehicleId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((serviceId == null) ? 0 : serviceId.hashCode());
		result = prime * result
				+ ((vehicleId == null) ? 0 : vehicleId.hashCode());
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
		ServiceVehicleConstraint other = (ServiceVehicleConstraint) obj;
		if (serviceId == null) {
			if (other.serviceId != null)
				return false;
		} else if (!serviceId.equals(other.serviceId))
			return false;
		if (vehicleId == null) {
			if (other.vehicleId != null)
				return false;
		} else if (!vehicleId.equals(other.vehicleId))
			return false;
		return true;
	}

}
