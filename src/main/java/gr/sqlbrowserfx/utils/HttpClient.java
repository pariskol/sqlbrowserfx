package gr.sqlbrowserfx.utils;

import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

public class HttpClient {

    private static volatile ExecutorService EXECUTOR_SERVICE = null;
    private static String BASIC_AUTH = "";
    private static OkHttpClient client = new OkHttpClient();
	private static boolean checkForCreds = false;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * Executes given runnable, in separate thread from HTTPClient fixed thread pool.
     * HTTPClient's pool is initialized once during 1st call of this function,
     * you can use this pool to execute your requests asynchronously.
     *
     * @param runnable
     */
    public static void executeAsync(Runnable runnable) {
        if (EXECUTOR_SERVICE == null)
            EXECUTOR_SERVICE = Executors.newFixedThreadPool(2);
        EXECUTOR_SERVICE.execute(runnable);
    }

    public static String GET(String url) throws IOException {
        checkForCredentials();
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization",  BASIC_AUTH)
                .build();

        Response response = client.newCall(request).execute();
        try (ResponseBody resBody = response.body()){
            String res = resBody.string();
            if (!response.isSuccessful())
                throw new IOException("Status code: " + response.code() + " , " + res);
            return resBody.string();
        }
    }

    private static void checkForCredentials() throws IllegalStateException{
        if (checkForCreds  && BASIC_AUTH == null)
            throw new IllegalStateException("This HTTPClient demands a Basic Authentication header to be set," +
                    " in order this to be done you must first call 'setBasicAuthCredentials' method");
    }

    public static String POST(String url, JSONObject json) throws IOException {
        checkForCredentials();
        json = json == null ? new JSONObject() : json;
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization",  BASIC_AUTH)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();

        try (ResponseBody resBody = response.body()){
            String res = resBody.string();
            if (!response.isSuccessful())
                throw new IOException("Status code: " + response.code() + " , " + res);
            return resBody.string();
        }
    }

    public static void setBasicAuthCredentials(String username, String password) {
        String userCredentials = username + ":" + password;
        BASIC_AUTH = "Basic " + new String(Base64.getEncoder().encode(userCredentials.getBytes()));
    }

}
