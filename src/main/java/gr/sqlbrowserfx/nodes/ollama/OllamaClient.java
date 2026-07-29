package gr.sqlbrowserfx.nodes.ollama;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;

import gr.sqlbrowserfx.utils.HttpClient;
import gr.sqlbrowserfx.utils.PropertiesLoader;

public class OllamaClient {
    private static final Gson GSON = new Gson();
    private final String baseUrl = PropertiesLoader.getProperty("ollama.url", String.class, "http://localhost:11434");
    private final HttpClient httpClient;

    public OllamaClient() {
        this.httpClient = new HttpClient();
    }

    // ---------------- API ----------------
    public List<Model> listModels() {
        try {
            var res = httpClient.get(baseUrl + "/api/tags");
            var json = new JSONObject(res);

            var modelsArray = json.getJSONArray("models");

            var models = new ArrayList<Model>();

            if (modelsArray != null) {
                for (int i = 0; i < modelsArray.length(); i++) {
                    models.add(GSON.fromJson(modelsArray.getJSONObject(i).toString(), Model.class));
                }
            }

            return models;

        } catch (Exception e) {
            throw new RuntimeException("Failed to list models", e);
        }
    }

    public HttpResponse<InputStream> generateStream(String model, String message) {
        var msg = new Message();
        msg.setRole("user");
        msg.setContent(message);
        return generateStream(model, List.of(msg));
    }

    public HttpResponse<InputStream> generateStream(String model, List<Message> messages) {
        try {
            var ollamaMessages = new JSONArray();

            ollamaMessages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", "You are a fast concise assistant. Respond briefly and accurately."));

            for (var msg : messages) {
                var content = new StringBuilder()
                        .append(msg.getMetaData() != null ? msg.getMetaData() : "")
                        .append(msg.getContent() != null ? msg.getContent() : "")
                        .toString();

                ollamaMessages.put(new JSONObject()
                        .put("role", msg.getRole()) // supported roles system (config), user, assistant
                        .put("content", content));
            }

            // Optimized inference options
            var options = new JSONObject()
                    //                    .put("num_predict", 50) // this limits response size
                    .put("temperature", 0.7)
                    .put("top_p", 0.9)
                    // Performance-related
                    .put("num_ctx", 2048)
                    .put("num_batch", 256)
                    // CPU thread count (ignored on some GPU setups)
                    .put("num_thread", Runtime.getRuntime().availableProcessors());

            var req = new JSONObject()
                    .put("model", model)
                    .put("messages", ollamaMessages)
                    .put("keep_alive", -1)
                    .put("stream", true)
                    .put("options", options);

            return httpClient.postStream(baseUrl + "/api/chat", req);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate response", e);
        }
    }

    public String generate(String model, String prompt) throws URISyntaxException, IOException, InterruptedException {
        String url = new StringBuilder(baseUrl)
                .append("/api/generate")
                .toString();

        String body = new JSONObject().put("model", model)
                .put("prompt", prompt)
                .put("stream", false)
                .toString();

        return new JSONObject(httpClient.post(url, body)).getString("response");
    }
}