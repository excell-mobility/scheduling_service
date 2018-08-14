package scheduling.connector;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
//import java.security.SecureRandom;
//import java.security.cert.CertificateException;
//import java.security.cert.X509Certificate;
//
//import javax.net.ssl.HostnameVerifier;
//import javax.net.ssl.HttpsURLConnection;
//import javax.net.ssl.KeyManager;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLSession;
//import javax.net.ssl.TrustManager;
//import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONArray;
import org.json.JSONObject;

public class HttpConnector implements HttpConnectorInterface {
	
	public HttpConnector() {}

	public synchronized String sendMessage(String urlStr, String channel,
			String contactId, String content) throws MalformedURLException,
			IOException {

		// set up request body and escape parameters for json
		String urlParameters = "{" + "\"channel\"" + ":" + "\"" + channel
				+ "\"" + "," + "\"contactId\"" + ":" + "\"" + contactId + "\""
				+ "," + "\"content\"" + ":" + "\"" + content + "\"" + "}";
		return this.getConnectionString(urlStr, urlParameters, RestRequestType.POST);

	}
	
	public synchronized String getConnectionAuthenticationString(String urlStr,
			String authenticationJSON)
			throws MalformedURLException, IOException, NoSuchAlgorithmException, KeyManagementException {
		
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		
		HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr)
				.openConnection();

		conn.setHostnameVerifier(allHostsValid);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json");

		// Send post request
		conn.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
		wr.writeBytes(authenticationJSON);
		wr.flush();
		wr.close();

		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}

		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();

		conn.disconnect();
		return sb.toString();

	}

	public synchronized String getConnectionString(String urlStr,
			String urlParameters, RestRequestType restRequestType)
			throws MalformedURLException, IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Accept", "application/json");
		switch (restRequestType) {
		case GET:
			conn.setRequestMethod("GET");
			conn.setRequestProperty("REFERER", "<enter your ip here>");
			break;
		
		case POST:
			conn.setRequestMethod("POST");

			// Send post request
			conn.setDoOutput(true);
			DataOutputStream postStream = new DataOutputStream(conn.getOutputStream());
			postStream.writeBytes(urlParameters);
			postStream.flush();
			postStream.close();
			break;
		
		case PUT:
			conn.setRequestMethod("PUT");

			// Send put request
			conn.setDoOutput(true);
			DataOutputStream putStream = new DataOutputStream(conn.getOutputStream());
			putStream.writeBytes(urlParameters);
			putStream.flush();
			putStream.close();
			break;
		}


		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}

		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();

		conn.disconnect();
		return sb.toString();

	}

	public synchronized String getConnectionStringWithToken(String urlStr,
			String token) throws MalformedURLException, IOException, NoSuchAlgorithmException, KeyManagementException {

		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		HttpsURLConnection conn;
		int counter = 0;
		// do maximum three request, if there is a timeout or another type of
		// error
		do {
			conn = (HttpsURLConnection) new URL(urlStr).openConnection();
			// disable ssl verification
			conn.setHostnameVerifier(allHostsValid);
			conn.setRequestMethod("GET");
			conn.setRequestProperty("REFERER", "<enter your ip here>");
			conn.setRequestProperty("Authorization", token);
			counter++;
		} while (conn.getResponseCode() != 200 && counter < 4);

		/*
		 * // alternative workflow for testing https connections // scroll down
		 * for DefaultTrustManager code SSLContext ctx =
		 * SSLContext.getInstance("TLS"); ctx.init(new KeyManager[0], new
		 * TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
		 * SSLContext.setDefault(ctx);
		 * 
		 * HttpsURLConnection conn = (HttpsURLConnection) new
		 * URL(urlStr).openConnection();
		 * 
		 * conn.setHostnameVerifier(new HostnameVerifier() { public boolean
		 * verify(String arg0, SSLSession arg1) { return true; } });
		 */

		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}

		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();

		conn.disconnect();
		return sb.toString();

	}

	public synchronized String getConnectionString(String urlStr)
			throws MalformedURLException, IOException {

		HttpURLConnection conn;
		int counter = 0;
		// do maximum three request, if there is a timeout or another type of
		// error
		do {
			conn = (HttpURLConnection) new URL(urlStr).openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("REFERER", "<enter your ip here>");
			counter++;
		} while (conn.getResponseCode() != 200 && counter < 4);

		/*
		 * // alternative workflow for testing https connections // scroll down
		 * for DefaultTrustManager code SSLContext ctx =
		 * SSLContext.getInstance("TLS"); ctx.init(new KeyManager[0], new
		 * TrustManager[] {new DefaultTrustManager()}, new SecureRandom());
		 * SSLContext.setDefault(ctx);
		 * 
		 * HttpsURLConnection conn = (HttpsURLConnection) new
		 * URL(urlStr).openConnection();
		 * 
		 * conn.setHostnameVerifier(new HostnameVerifier() { public boolean
		 * verify(String arg0, SSLSession arg1) { return true; } });
		 */

		if (conn.getResponseCode() != 200) {
			throw new IOException(conn.getResponseMessage());
		}

		// Buffer the result into a string
		BufferedReader rd = new BufferedReader(new InputStreamReader(
				conn.getInputStream()));
		StringBuilder sb = new StringBuilder();
		String line;
		while ((line = rd.readLine()) != null) {
			sb.append(line);
		}
		rd.close();

		conn.disconnect();
		return sb.toString();

	}

	public JSONArray getJSONArrayResult(String result) {
		JSONArray jsonArray = new JSONArray(result);
		return jsonArray;
	}

	public JSONObject getJSONObjectResult(String result) {
		JSONObject jsonObj = new JSONObject(result);
		return jsonObj;

	}

	/*
	 * private static class DefaultTrustManager implements X509TrustManager {
	 * 
	 * public void checkClientTrusted(X509Certificate[] arg0, String arg1)
	 * throws CertificateException {}
	 * 
	 * public void checkServerTrusted(X509Certificate[] arg0, String arg1)
	 * throws CertificateException {}
	 * 
	 * public X509Certificate[] getAcceptedIssuers() { return null; } }
	 */

	// public synchronized JSONObject getJSONObjectResult(String urlStr, String
	// urlParameters, boolean isGetRequest) throws IOException {
	// String result = this.getConnectionString(urlStr, urlParameters,
	// isGetRequest);
	// JSONObject jsonObj = new JSONObject(result);
	// return jsonObj;
	// }

	// private synchronized JSONObject getJSONObjectResult(String result) throws
	// IOException, RoutingNotFoundException {
	//
	// JSONObject jsonObj = null;
	//
	// if (result == null || result.equals(""))
	// throw new
	// RoutingNotFoundException("There's a problem with the connection");
	// // jsonObj = new JSONObject();
	// else
	// jsonObj = new JSONObject(result);
	//
	// return jsonObj;
	//
	// }
}
