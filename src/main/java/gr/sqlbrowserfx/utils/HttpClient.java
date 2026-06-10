package gr.sqlbrowserfx.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.json.JSONObject;


public class HttpClient {

	private Integer defautTimeout = (Integer) PropertiesLoader.getProperty("http.timeout", Integer.class, 10);
	private java.net.http.HttpClient client;
	private Map<String, String> headers = new HashMap<>();

	public HttpClient() {
		client = java.net.http.HttpClient.newBuilder()
				.connectTimeout(Duration.of(defautTimeout, ChronoUnit.SECONDS))
				.build();
	}

	public String get(String url) throws IOException, InterruptedException, URISyntaxException {
		var builder = HttpRequest.newBuilder()
						.uri(new URI(url))
						.GET();
		this.headers.entrySet().forEach(header -> builder.header(header.getKey(), header.getValue()));
		var req = builder.build();
		var res = client.send(req, BodyHandlers.ofString());
		
		checkResSuccess(res);
		
		return res.body().toString();
	}
	
	public String getSafely(String url) {
		try {
			return get(url);
		} catch(Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public String post(String url, Object body) throws URISyntaxException, IOException, InterruptedException {
		var bodyPublisher = BodyPublishers.ofString(body instanceof JSONObject ? body.toString(): body instanceof String ? (String) body : new JSONObject(body).toString());
		var builder = HttpRequest.newBuilder()
						.uri(new URI(url))
						.POST(bodyPublisher);
		this.headers.entrySet().forEach(header -> builder.header(header.getKey(), header.getValue()));
		var req = builder.build();
		var res = client.send(req, BodyHandlers.ofString());
		
		checkResSuccess(res);
		
		return res.body().toString();
	}

	/**
	 * Adds header globally to every request that will be performed after. A header
	 * can be overrided by calling this method again.
	 * 
	 * @param key
	 * @param value
	 */
	public HttpClient addHeader(String key, String value) {
		this.headers.put(key, value);
		return this;
	}

	/**
	 * Removes header globally from every request that will be performed after.
	 * 
	 * @param key
	 * @param value
	 */
	public void removeHeader(String key) {
		this.headers.remove(key);
	}

	public void clearHeaders() {
		this.headers.clear();
	}

	private void checkResSuccess(HttpResponse<?> res) throws IOException {
		if (res.statusCode() < 200 || res.statusCode() > 300) {
			throw new IOException("Status code: " + res.statusCode() + " , " + res.body().toString());
		}
	}
	
	public static String mapToFormUrlEncoded(Object object) {
	    String form = new JSONObject(object).toMap().entrySet()
		        .stream()
		        .map(e -> e.getKey() + "=" + URLEncoder.encode(String.valueOf(e.getValue()), StandardCharsets.UTF_8))
		        .collect(Collectors.joining("&"));
	    return form;
	}
	
	public HttpResponse<InputStream> postStream(String url, Object body)
	        throws Exception {

	    var bodyPublisher = BodyPublishers.ofString(
	        body instanceof JSONObject
	            ? body.toString()
	            : new JSONObject(body).toString()
	    );

	    var builder = HttpRequest.newBuilder()
	            .uri(new URI(url))
	            .POST(bodyPublisher);

	    this.headers.forEach(builder::header);

	    var req = builder.build();

	    return client.send(
	        req,
	        BodyHandlers.ofInputStream()
	    );
	}
}
