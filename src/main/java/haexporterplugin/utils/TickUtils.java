package haexporterplugin.utils;

import haexporterplugin.HAExporterConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class TickUtils {
    @Getter
    private int tickCount = 0;

    @Inject
    private MessageBuilder messageBuilder;

    @Inject
    private HomeAssistUtils homeAssistUtils;

    @Inject
    private HAExporterConfig config;

    public void onTick(){
        tickCount++;
//        log.debug(String.valueOf(tickCount));
    }

    public void sendOnSendRate(){
        if (tickCount >= config.sendRate()){
            tickCount = 0;
            String json = messageBuilder.build();
            homeAssistUtils.sendMessage(json);
        }
    }

    public void sendNow(){
        tickCount = 0;
        String json = messageBuilder.build();
        homeAssistUtils.sendMessage(json);
    }
}
