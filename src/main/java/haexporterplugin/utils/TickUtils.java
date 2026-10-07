package haexporterplugin.utils;

import haexporterplugin.HAExporterConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

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

    @Inject
    private Client client;

    public void onTick(){
        tickCount++;
    }

    public void sendOnSendRate(){
        if (tickCount >= config.sendRate()){
            tickCount = 0;
            send();
        }
    }

    public void sendNow(){
        tickCount = 0;
        send();
    }

    public void sendShutdown(){
        send();
    }

    // Events and the location trail are cleared as soon as they're serialized, so each is part of one message only.
    // HomeAssistUtils keeps that message for every connection it could not be delivered to
    private void send(){
        if (dropOnSpecialWorld()) return;
        String json = messageBuilder.build();
        messageBuilder.resetEvents();
        messageBuilder.resetLocationTrail();
        homeAssistUtils.sendMessage(json);
    }

    // Nothing is sent from special worlds unless the user opted in. Pending events and trail points are
    // dropped too, so they can't show up later in a message from a normal world.
    private boolean dropOnSpecialWorld(){
        if (config.sendSpecialWorldData() || !WorldUtils.isSpecialWorld(client.getWorldType())){
            return false;
        }
        messageBuilder.resetEvents();
        messageBuilder.resetLocationTrail();
        return true;
    }
}
