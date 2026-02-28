package haexporterplugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.data.HAConnection;
import haexporterplugin.data.TokenCallback;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.internal.annotations.EverythingIsNonNull;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Slf4j
@Singleton
public class HomeAssistUtils {
    private static final String DATA_ENDPOINT = "/api/osrs-data/events";
    private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
    private static final String TOKEN_HEADER = "X-Osrs-Token";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    @Inject
    protected HAExporterConfig config;

    @Inject
    private ConfigUtils configUtils;

    @Inject
    private OkHttpClient okHttpClient;

    private @Inject Gson gson;

    public void sendMessage(String jsonPayload) {
        sendPayload(jsonPayload);
    }


    private void sendPayload(String jsonPayload) {
        List<HAConnection> connections = configUtils.getStoredConnections();

        for (HAConnection connection : connections) {
            String apiUrl = connection.getBaseUrl() + DATA_ENDPOINT;
            Request request = buildRequest(apiUrl, jsonPayload, connection.token);

            okHttpClient.newCall(request).enqueue(createCallback(jsonPayload));
        }
    }

    private Request buildRequest(String apiUrl, String jsonPayload, String token) {
        RequestBody requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE_JSON), jsonPayload);

        return new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.parse(apiUrl)))
                .header(TOKEN_HEADER, token)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                .post(requestBody)
                .build();
    }

    private Callback createCallback(String jsonPayload) {
        return new Callback() {
            @Override
            @EverythingIsNonNull
            public void onFailure(Call call, IOException e) {
                log.error("Error submitting the entity to homeassistant ", e);
            }

            @Override
            @EverythingIsNonNull
            public void onResponse(Call call, Response response) {
                response.close();
            }
        };
    }

    public void getToken(String baseUrl, String code, TokenCallback callback) {
        String apiUrl = baseUrl + "/api/osrs-data/pair";

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("code", code);

        String jsonPayload = gson.toJson(jsonObject);

        RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), jsonPayload);

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
                log.error("Error acquiring token", e);
                callback.onFailure(e);
            }

            @Override
            public void onResponse(@Nonnull Call call, @Nonnull Response response) {
                try (ResponseBody body = response.body()) {

                    if (!response.isSuccessful()) {
                        callback.onFailure(new IOException("Unexpected response " + response));
                        return;
                    }

                    if (body == null) {
                        callback.onFailure(new IOException("Empty response body"));
                        return;
                    }

                    String responseBody = body.string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                    String token = jsonResponse.get("token").getAsString();
                    callback.onSuccess(token);

                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }
        });
    }
}
