package haexporterplugin.utils;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Singleton
public class MessageBuilder {

    @Inject
    private Gson gson;

    Map<String, Object> categories = new LinkedHashMap<>();

    public void setData(String category, Object data)
    {
        log.debug("Adding data in {}: {}", category, data.toString());
        categories.put(category, data);
    }

    public String build()
    {
        log.debug("Converting data");
        return gson.toJson(categories);
    }

    public void resetData(){
        categories.clear();
    }
}
