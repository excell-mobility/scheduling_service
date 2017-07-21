package scheduling.model;

import java.util.List;
import java.util.Map;

public class CareScenarioResponse {

	private Map<String, List<String>> response;

	public CareScenarioResponse(Map<String, List<String>> response) {
		this.response = response;
	}

	public Map<String, List<String>> getResponse() {
		return response;
	}

	public void setResponse(Map<String, List<String>> response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return "CareScenarioResponse [response=" + response + "]";
	}
	
}
