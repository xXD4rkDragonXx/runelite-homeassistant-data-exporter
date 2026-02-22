package haexporterplugin.notifiers;

import haexporterplugin.HAExporterConfig;
import haexporterplugin.utils.*;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;

@Slf4j
public abstract class BaseNotifier {

    @Inject
    protected Client client;

    @Inject
    protected HAExporterConfig config;

    @Inject
    protected HomeAssistUtils homeAssistUtils;

    @Inject
    protected TickUtils tickUtils;

    @Inject
    protected RarityUtils rarityUtils;

    @Inject
    protected ThievingUtils thievingUtils;

    @Inject
    protected MessageBuilder messageBuilder;

}
