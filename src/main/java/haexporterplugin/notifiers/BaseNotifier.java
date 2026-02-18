package haexporterplugin.notifiers;

import haexporterplugin.HAExporterConfig;
import haexporterplugin.utils.HomeAssistUtils;
import haexporterplugin.utils.MessageBuilder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;

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
    protected MessageBuilder messageBuilder;
}
