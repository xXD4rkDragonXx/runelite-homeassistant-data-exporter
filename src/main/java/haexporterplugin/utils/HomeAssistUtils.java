package haexporterplugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.data.TokenCallback;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Slf4j
@Singleton
public class HomeAssistUtils {
    @Inject
    protected HAExporterConfig config;

    @Inject
    private OkHttpClient okHttpClient;

    public void sendMessage(String jsonPayload) {
        String homeAssistantUrl = config.homeassistantUrl();
        String accessToken = config.homeassistantToken();

        if (homeAssistantUrl.isEmpty() || accessToken.isEmpty()) {
            log.warn("Home Assistant URL or Access Token not configured.");
            return;
        }

        String apiUrl = homeAssistantUrl + "/api";
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), jsonPayload);
        log.debug("Sending payload to home assistant, {}: {}", apiUrl, jsonPayload);
        Request request = new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.parse(apiUrl)))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log.error("Error submitting the entity to homeassistant ", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                log.info("Successfully created/updated entity {}.", jsonPayload);
                response.close();
            }
        });
    }

    public void getToken(String baseUrl, String code, TokenCallback callback) {
        String apiUrl = baseUrl + "/api/osrs-data/pair";

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("code", code);

        Gson gson = new Gson();
        String jsonPayload = gson.toJson(jsonObject);

        RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), jsonPayload        );

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
