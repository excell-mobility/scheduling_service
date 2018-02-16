package scheduling.model;

import java.util.List;
import java.util.Map;

import beans.TransportProperties;

public class PickupScenarioResponse {

	private Map<String, List<TransportProperties>> response;

	public PickupScenarioResponse(Map<String, List<TransportProperties>> response) {
		this.response = response;
	}

	public Map<String, List<TransportProperties>> getResponse() {
		return response;
	}

	public void setResponse(Map<String, List<TransportProperties>> response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return "CareScenarioResponse [response=" + response + "]";
	}
	
}
