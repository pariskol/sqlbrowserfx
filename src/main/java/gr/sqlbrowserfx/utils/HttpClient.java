package gr.sqlbrowserfx.utils;

import java.io.IOException;

import org.json.JSONObject;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

public class HttpClient {

    private static OkHttpClient client = new OkHttpClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    public static String GET(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        try (ResponseBody resBody = response.body()){
            String res = resBody.string();
            if (!response.isSuccessful())
                throw new IOException("Status code: " + response.code() + " , " + res);
            return res;
        }
    }

    public static String POST(String url, JSONObject json) throws IOException {
        json = json == null ? new JSONObject() : json;
        RequestBody body = RequestBody.create(JSON, json.toString());
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();

        try (ResponseBody resBody = response.body()){
            String res = resBody.string();
            if (!response.isSuccessful())
                throw new IOException("Status code: " + response.code() + " , " + res);
            return res;
        }
    }

}
