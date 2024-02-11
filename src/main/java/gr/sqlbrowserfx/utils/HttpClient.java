package gr.sqlbrowserfx.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.json.JSONObject;

public class HttpClient {

	private static final java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder().build();

	public static String GET(String url) throws IOException, InterruptedException, URISyntaxException {
		var builder = HttpRequest.newBuilder().uri(new URI(url)).GET();
		var req = builder.build();
		var res = client.send(req, BodyHandlers.ofString());

		checkResSuccess(res);

		return res.body().toString();
	}

	public static String POST(String url, Object body) throws URISyntaxException, IOException, InterruptedException {
		var bodyPublisher = BodyPublishers
				.ofString(body instanceof JSONObject ? body.toString() : new JSONObject(body).toString());
		var builder = HttpRequest.newBuilder().uri(new URI(url)).POST(bodyPublisher);
		var req = builder.build();
		var res = client.send(req, BodyHandlers.ofString());

		checkResSuccess(res);

		return res.body().toString();
	}

	private static void checkResSuccess(HttpResponse<?> res) throws IOException {
		if (res.statusCode() < 200 || res.statusCode() > 300) {
			throw new IOException("Status code: " + res.statusCode() + " , " + res.body().toString());
		}
	}
}
