package scheduling.beans;

import java.io.Serializable;

public class ServiceOrderConstraint implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String beforeServiceID;
	private String afterServiceID;
	
	public ServiceOrderConstraint(String beforeServiceID, String afterServiceID) {
		this.beforeServiceID = beforeServiceID;
		this.afterServiceID = afterServiceID;
	}

	public String getBeforeServiceID() {
		return beforeServiceID;
	}

	public void setBeforeServiceID(String beforeServiceID) {
		this.beforeServiceID = beforeServiceID;
	}

	public String getAfterServiceID() {
		return afterServiceID;
	}

	public void setAfterServiceID(String afterServiceID) {
		this.afterServiceID = afterServiceID;
	}

	@Override
	public String toString() {
		return "ServiceOrderConstraint [beforeServiceID=" + beforeServiceID + ", afterServiceID="
				+ afterServiceID + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((afterServiceID == null) ? 0 : afterServiceID.hashCode());
		result = prime * result
				+ ((beforeServiceID == null) ? 0 : beforeServiceID.hashCode());
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
		ServiceOrderConstraint other = (ServiceOrderConstraint) obj;
		if (afterServiceID == null) {
			if (other.afterServiceID != null)
				return false;
		} else if (!afterServiceID.equals(other.afterServiceID))
			return false;
		if (beforeServiceID == null) {
			if (other.beforeServiceID != null)
				return false;
		} else if (!beforeServiceID.equals(other.beforeServiceID))
			return false;
		return true;
	}

}
