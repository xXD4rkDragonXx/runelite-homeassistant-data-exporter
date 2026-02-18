package haexporterplugin.utils;

import haexporterplugin.HAExporterConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.inject.Inject;
import java.io.IOException;
import java.util.*;

@Slf4j
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
}
