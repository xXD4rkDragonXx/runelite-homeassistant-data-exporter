package haexporterplugin.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.HAExporterPlugin;
import haexporterplugin.data.HAConnection;
import haexporterplugin.data.PairingException;
import haexporterplugin.data.TokenCallback;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.internal.annotations.EverythingIsNonNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class HomeAssistUtils {
    private static final String DATA_ENDPOINT = "/api/osrs-data/events";
    private static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
    private static final String TOKEN_HEADER = "X-Osrs-Token";
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";
    private static final String VERSION_HEADER = "X-Osrs-Exporter-Version";

    // Limits for text an endpoint sends back during pairing (shown in the panel / dialogs)
    private static final int MAX_PAIR_NAME_LENGTH = 64;
    private static final int MAX_PAIR_ERROR_LENGTH = 200;
    private static final long MAX_PAIR_ERROR_BODY_BYTES = 8 * 1024;

    @Inject
    protected HAExporterConfig config;

    @Inject
    private ConfigUtils configUtils;

    @Inject
    private OkHttpClient okHttpClient;

    @Inject
    private ScheduledExecutorService executor;

    private @Inject Gson gson;

    private final ConnectionBackoff backoff = new ConnectionBackoff();

    // Set while the plugin is disabled, so late callbacks and scheduled drains don't queue or resend anything
    private volatile boolean stopped;

    public void startUp() {
        stopped = false;
    }

    public void shutDown() {
        stopped = true;
        backoff.clearAll();
    }

    public void sendMessage(String jsonPayload) {
        sendPayload(jsonPayload);
    }

    public long getPausedUntil(HAConnection connection) {
        return backoff.getPausedUntil(connectionKey(connection));
    }

    public int getQueuedCount(HAConnection connection) {
        return backoff.getQueuedCount(connectionKey(connection));
    }

    private void sendPayload(String jsonPayload) {
        List<HAConnection> connections = configUtils.getStoredConnections();

        for (HAConnection connection : connections) {
            String key = connectionKey(connection);
            if (!connection.isEnabled()) {
                log.debug("Skipping disabled connection: {}", connection.getDisplayName());
                backoff.clear(key);
                continue;
            }

            String filteredPayload = applyConnectionFilters(jsonPayload, connection);

            if (backoff.isPaused(key)) {
                // Events wait for the pause to end; plain snapshots are dropped, the next one carries the full state anyway
                if (payloadHasEvents(filteredPayload)) {
                    backoff.enqueue(key, filteredPayload);
                    log.debug("{} is paused, queued payload for retry", connection.getDisplayName());
                } else {
                    log.debug("{} is paused, dropped snapshot", connection.getDisplayName());
                }
                continue;
            }

            if (backoff.getQueuedCount(key) > 0 && payloadHasEvents(filteredPayload)) {
                // Line up behind the payloads still waiting to be resent, so events arrive in order
                backoff.enqueue(key, filteredPayload);
            } else {
                sendToConnection(connection, key, filteredPayload, false);
            }

            // Resends queued payloads once a pause has ended; a no-op when nothing is queued or a resend is in flight
            drainQueued(key);
        }
    }

    private void sendToConnection(HAConnection connection, String key, String payload, boolean drained) {
        String apiUrl = connection.getBaseUrl() + DATA_ENDPOINT;
        Request request = buildRequest(apiUrl, payload, connection.token);

        if (log.isDebugEnabled()){
            log.debug("{} ({}): {}",connection.getDisplayName(), apiUrl, payload);
        }

        okHttpClient.newCall(request).enqueue(createCallback(payload, connection, key, drained));
    }

    private void drainQueued(String key) {
        if (stopped) {
            return;
        }

        String payload = backoff.beginDrain(key);
        if (payload == null) {
            return;
        }

        HAConnection connection = findConnection(key);
        if (connection == null || !connection.isEnabled()) {
            // Removed or disabled since the payload was queued
            backoff.clear(key);
            return;
        }

        sendToConnection(connection, key, payload, true);
    }

    private void scheduleDrain(String key, long pausedUntil) {
        long delay = Math.max(0, pausedUntil - System.currentTimeMillis());
        executor.schedule(() -> {
            long stillPausedUntil = backoff.getPausedUntil(key);
            if (stillPausedUntil > 0) {
                // Woke up slightly early, or the pause was extended meanwhile
                scheduleDrain(key, stillPausedUntil);
            } else {
                drainQueued(key);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    private HAConnection findConnection(String key) {
        for (HAConnection connection : configUtils.getStoredConnections()) {
            if (connectionKey(connection).equals(key)) {
                return connection;
            }
        }
        return null;
    }

    private static String connectionKey(HAConnection connection) {
        return connection.getBaseUrl() + '\n' + connection.getToken();
    }

    private boolean payloadHasEvents(String payload) {
        JsonObject root = gson.fromJson(payload, JsonObject.class);
        JsonElement events = root != null ? root.get("events") : null;
        return events != null && events.isJsonArray() && events.getAsJsonArray().size() > 0;
    }

    private String applyConnectionFilters(String jsonPayload, HAConnection connection) {
        boolean allDataEnabled = connection.isIncludeInventory()
                && connection.isIncludeEquipment()
                && connection.isIncludeLocation()
                && config.includeInventory()
                && config.includeEquipment()
                && config.includeLocation();

        boolean allEventsEnabled = connection.isIncludeLootEvents() && config.includeLootEvents()
                && connection.isIncludeDeathEvents() && config.includeDeathEvents()
                && connection.isIncludeLevelUpEvents() && config.includeLevelUpEvents()
                && connection.isIncludeAchievementDiaryEvents() && config.includeAchievementDiaryEvents()
                && connection.isIncludeCombatTaskEvents() && config.includeCombatTaskEvents()
                && connection.isIncludeSuperiorEvents() && config.includeSuperiorEvents()
                && connection.isIncludeCollectionLogEvents() && config.includeCollectionLogEvents();

        if (allDataEnabled && allEventsEnabled) {
            return jsonPayload;
        }

        JsonObject root = gson.fromJson(jsonPayload, JsonObject.class);

        if (!allDataEnabled && root.has("player")) {
            JsonObject player = root.getAsJsonObject("player");
            if (!connection.isIncludeInventory() || !config.includeInventory()) {
                player.remove("inventory");
            }
            if (!connection.isIncludeEquipment() || !config.includeEquipment()) {
                player.remove("equipment");
            }
            if (!connection.isIncludeLocation() || !config.includeLocation()) {
                player.remove("location");
            }
        }

        if (!allEventsEnabled && root.has("events")) {
            JsonArray filtered = new JsonArray();
            for (JsonElement element : root.getAsJsonArray("events")) {
                JsonObject event = element.getAsJsonObject();
                String type = event.has("type") ? event.get("type").getAsString() : "";
                if (shouldIncludeEvent(type, connection)) {
                    filtered.add(event);
                }
            }
            root.add("events", filtered);
        }

        return gson.toJson(root);
    }

    private boolean shouldIncludeEvent(String type, HAConnection connection) {
        switch (type) {
            case "loot":
            case "pkLoot":
                return connection.isIncludeLootEvents() && config.includeLootEvents();
            case "death":
                return connection.isIncludeDeathEvents() && config.includeDeathEvents();
            case "levelUp":
                return connection.isIncludeLevelUpEvents() && config.includeLevelUpEvents();
            case "achievementDiary":
                return connection.isIncludeAchievementDiaryEvents() && config.includeAchievementDiaryEvents();
            case "combatTask":
                return connection.isIncludeCombatTaskEvents() && config.includeCombatTaskEvents();
            case "superiorSpawn":
                return connection.isIncludeSuperiorEvents() && config.includeSuperiorEvents();
            case "collectionLog":
                return connection.isIncludeCollectionLogEvents() && config.includeCollectionLogEvents();
            default:
                // clientShutdown and any unknown events are always forwarded
                return true;
        }
    }

    Request buildRequest(String apiUrl, String jsonPayload, String token) {
        RequestBody requestBody = RequestBody.create(MediaType.parse(CONTENT_TYPE_JSON), jsonPayload);

        return new Request.Builder()
                .url(Objects.requireNonNull(HttpUrl.parse(apiUrl)))
                .header(TOKEN_HEADER, token)
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON)
                .header(VERSION_HEADER, HAExporterPlugin.PLUGIN_VERSION)
                .post(requestBody)
                .build();
    }

    private Callback createCallback(String jsonPayload, HAConnection connection, String key, boolean drained) {
        return new Callback() {
            @Override
            @EverythingIsNonNull
            public void onFailure(Call call, IOException e) {
                retryLater(connection, key, jsonPayload, drained, null, e.toString());
            }

            @Override
            @EverythingIsNonNull
            public void onResponse(Call call, Response response) {
                try {
                    int code = response.code();
                    switch (ConnectionBackoff.classify(code)) {
                        case SUCCESS:
                            backoff.recordSuccess(key);
                            if (drained) {
                                backoff.completeDrain(key, true);
                            }
                            drainQueued(key);
                            break;
                        case UNAUTHORIZED:
                            log.warn("Received 401 Unauthorized from {}. Disabling connection.", connection.getDisplayName());
                            disableConnection(connection, "Unauthorized (401): Token may have been revoked.");
                            break;
                        case GONE:
                            log.warn("Received 410 Gone from {}. Disabling connection.", connection.getDisplayName());
                            disableConnection(connection, "Endpoint gone (410): this endpoint no longer accepts data.");
                            break;
                        case RETRY_AFTER:
                            Long retryAt = ConnectionBackoff.parseRetryAfter(response.header(RETRY_AFTER_HEADER), System.currentTimeMillis());
                            retryLater(connection, key, jsonPayload, drained, retryAt, "HTTP " + code);
                            break;
                        case BACKOFF:
                            retryLater(connection, key, jsonPayload, drained, null, "HTTP " + code);
                            break;
                        default:
                            log.debug("{} rejected a payload with HTTP {}, not retrying it", connection.getDisplayName(), code);
                            if (drained) {
                                backoff.completeDrain(key, true);
                                drainQueued(key);
                            }
                            break;
                    }
                } finally {
                    response.close();
                }
            }
        };
    }

    /**
     * Pauses the connection after a failed delivery and keeps the payload for a retry when it carries events.
     * Receivers dedupe on eventId, so resending a payload that did arrive (e.g. after a timeout) is harmless.
     */
    private void retryLater(HAConnection connection, String key, String payload, boolean drained, Long retryAt, String cause) {
        if (stopped) {
            log.debug("Could not deliver data to {} ({}) after the plugin was disabled, not retrying", connection.getDisplayName(), cause);
            return;
        }

        boolean firstFailure = retryAt != null
                ? backoff.recordRetryAfter(key, retryAt)
                : backoff.recordFailure(key);
        long pausedUntil = backoff.getPausedUntil(key);

        if (drained) {
            backoff.completeDrain(key, false);
        } else if (payloadHasEvents(payload)) {
            backoff.enqueue(key, payload);
        }

        long pauseSeconds = (Math.max(0, pausedUntil - System.currentTimeMillis()) + 999) / 1000;
        if (firstFailure) {
            log.warn("Could not deliver data to {} ({}). Pausing for {}s before retrying.", connection.getDisplayName(), cause, pauseSeconds);
        } else {
            log.debug("Could not deliver data to {} ({}). Pausing for {}s before retrying.", connection.getDisplayName(), cause, pauseSeconds);
        }

        scheduleDrain(key, pausedUntil);
    }

    private void disableConnection(HAConnection connection, String reason) {
        backoff.clear(connectionKey(connection));

        List<HAConnection> connections = configUtils.getStoredConnections();
        for (HAConnection c : connections) {
            if (c.getBaseUrl().equals(connection.getBaseUrl())
                    && c.getToken().equals(connection.getToken())) {
                c.setEnabled(false);
                c.setDisabledReason(reason);
                break;
            }
        }
        configUtils.saveConnections(connections);
    }

    public void getToken(String baseUrl, String code, TokenCallback callback) {
        String apiUrl = baseUrl + "/api/osrs-data/pair";

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("code", code);

        String jsonPayload = gson.toJson(jsonObject);

        RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), jsonPayload);

        Request request = new Request.Builder()
                .url(apiUrl)
                .header(VERSION_HEADER, HAExporterPlugin.PLUGIN_VERSION)
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
                        String serverError = extractPairError(gson, readPairErrorBody(response));
                        callback.onFailure(new PairingException("Unexpected response " + response, serverError));
                        return;
                    }

                    if (body == null) {
                        callback.onFailure(new IOException("Empty response body"));
                        return;
                    }

                    String responseBody = body.string();
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);

                    String token = jsonResponse.get("token").getAsString();
                    callback.onSuccess(token, parsePairName(jsonResponse));

                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    // Best effort: an unreadable error body just means the generic failure message is shown.
    @Nullable
    private static String readPairErrorBody(Response response) {
        try {
            return response.peekBody(MAX_PAIR_ERROR_BODY_BYTES).string();
        } catch (Exception e) {
            return null;
        }
    }

    // Optional "name" from a successful pair response, used as the connection's default friendly name.
    @Nullable
    static String parsePairName(JsonObject pairResponse) {
        return sanitizeServerText(getStringField(pairResponse, "name"), MAX_PAIR_NAME_LENGTH, false);
    }

    // Optional "error" from a failed pair response. Returns null for anything unusable (non-JSON, no/non-string "error").
    @Nullable
    static String extractPairError(Gson gson, @Nullable String body) {
        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        try {
            JsonElement root = gson.fromJson(body, JsonElement.class);
            if (root == null || !root.isJsonObject()) {
                return null;
            }
            return sanitizeServerText(getStringField(root.getAsJsonObject(), "error"), MAX_PAIR_ERROR_LENGTH, true);
        } catch (RuntimeException e) {
            // Not JSON, e.g. an HTML error page from a reverse proxy
            return null;
        }
    }

    // Text from the network ends up in Swing labels and dialogs, which render text starting with "<html>" as HTML.
    // Strips control characters (optionally keeping '\n') and angle brackets, trims and caps the length. Blank -> null.
    @Nullable
    static String sanitizeServerText(@Nullable String text, int maxLength, boolean keepNewlines) {
        if (text == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '<' || c == '>' || (Character.isISOControl(c) && !(keepNewlines && c == '\n'))) {
                continue;
            }
            sb.append(c);
        }

        String result = sb.toString().trim();
        if (result.length() > maxLength) {
            // Don't split a surrogate pair at the cut
            int end = Character.isHighSurrogate(result.charAt(maxLength - 1)) ? maxLength - 1 : maxLength;
            result = result.substring(0, end).trim();
        }

        return result.isEmpty() ? null : result;
    }

    @Nullable
    private static String getStringField(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return null;
        }
        return element.getAsString();
    }
}
