package scheduling.connector;

import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONArray;
import org.json.JSONObject;

public interface HttpConnectorInterface {

    public String sendMessage(String urlStr, String channel, String contactId, String content) throws MalformedURLException, IOException;

    public String getConnectionString(String urlStr, String urlParameters, RestRequestType restRequestType) throws MalformedURLException, IOException;

    public String getConnectionString(String urlStr) throws MalformedURLException, IOException;

    public JSONObject getJSONObjectResult(String result);

    public JSONArray getJSONArrayResult(String result);

	public enum RestRequestType {
		GET,
		POST,
		PUT
	}
}
