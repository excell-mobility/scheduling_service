package scheduling.model;

import java.util.List;
import java.util.Map;

import beans.JobProperties;

public class CareScenarioResponse {

	private Map<String, List<JobProperties>> response;

	public CareScenarioResponse(Map<String, List<JobProperties>> response) {
		this.response = response;
	}

	public Map<String, List<JobProperties>> getResponse() {
		return response;
	}

	public void setResponse(Map<String, List<JobProperties>> response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return "CareScenarioResponse [response=" + response + "]";
	}
	
}
