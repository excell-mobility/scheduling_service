package scheduling.model;

import java.util.List;
import java.util.Map;

import scheduling.beans.Service;

public class CareScenarioResponse {

	private Map<String, List<Service>> response;

	public CareScenarioResponse(Map<String, List<Service>> response) {
		this.response = response;
	}

	public Map<String, List<Service>> getResponse() {
		return response;
	}

	public void setResponse(Map<String, List<Service>> response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return "CareScenarioResponse [response=" + response + "]";
	}
	
}
