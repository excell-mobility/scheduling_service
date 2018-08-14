package scheduling.beans;

import java.io.Serializable;

import io.swagger.annotations.ApiModelProperty;

public class GeoPoint implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@ApiModelProperty(notes = "Latitude coordinate in WGS84", required = true)
	private double latitude;
	@ApiModelProperty(notes = "Longitude coordinate in WGS84", required = true)
	private double longitude;
	
	public GeoPoint(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	@Override
	public String toString() {
		return "GeoPoint{" +
				"latitude=" + latitude +
				", longitude=" + longitude +
				'}';
	}

}
