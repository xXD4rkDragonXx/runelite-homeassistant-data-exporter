package haexporterplugin.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.data.HAConnection;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class ConfigUtils {
    private @Inject HAExporterConfig config;
    private @Inject Gson gson;

    public List<HAConnection> getStoredConnections()
    {
        Type listType = new TypeToken<List<HAConnection>>() {}.getType();

        try
        {
            List<HAConnection> connections =
                    gson.fromJson(config.homeassistantConnections(), listType);

            return connections != null ? connections : new ArrayList<>();
        }
        catch (Exception e)
        {
            return new ArrayList<>();
        }
    }

    public void addStoredConnection(String baseUrl, String token, @Nullable String friendlyName){
        Type listType = new TypeToken<List<HAConnection>>() {}.getType();

        List<HAConnection> connections;

        try
        {
            connections = gson.fromJson(config.homeassistantConnections(), listType);
            if (connections == null)
                connections = new ArrayList<>();
        }
        catch (Exception ex)
        {
            connections = new ArrayList<>();
        }

        HAConnection connection = new HAConnection(baseUrl, token);
        if (friendlyName != null)
            connection.setFriendlyName(friendlyName);

        connections.add(connection);
        config.setHomeassistantConnections(gson.toJson(connections));
    }

    public void saveConnections(List<HAConnection> connections)
    {
        config.setHomeassistantConnections(gson.toJson(connections));
    }
}
