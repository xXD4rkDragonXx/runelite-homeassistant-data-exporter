package haexporterplugin.notifiers;

import com.google.inject.Injector;
import haexporterplugin.HAExporterConfig;
import haexporterplugin.TestUtils;
import haexporterplugin.data.CollectionData;
import haexporterplugin.utils.KillCountTracker;
import haexporterplugin.utils.MessageBuilder;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.ScriptID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class CollectionNotifierTest
{
	private static final int WHIP_ID = 4;
	private static final int CHAT_ONLY = 1;
	private static final int POPUP = 2;

	private int tick = 1000;
	private final Client client = mock(Client.class);
	private final HAExporterConfig config = mock(HAExporterConfig.class);
	private MessageBuilder messageBuilder;
	private KillCountTracker killCountTracker;
	private CollectionNotifier collectionNotifier;

	@Before
	public void setUp()
	{
		when(client.getTickCount()).thenAnswer(inv -> tick);
		when(client.getItemCount()).thenReturn(10);

		ItemComposition whip = item("Abyssal whip");
		ItemComposition unnamed = item("null");
		ItemManager itemManager = mock(ItemManager.class);
		when(itemManager.canonicalize(anyInt())).thenAnswer(inv -> inv.getArgument(0));
		when(itemManager.getItemComposition(anyInt())).thenAnswer(inv -> (int) inv.getArgument(0) == WHIP_ID ? whip : unnamed);
		when(itemManager.getItemPrice(WHIP_ID)).thenReturn(1_500_000L);

		// Runs deferred work right away, so the tests stay synchronous
		ClientThread clientThread = mock(ClientThread.class);
		doAnswer(inv -> { inv.<Runnable>getArgument(0).run(); return null; }).when(clientThread).invokeAtTickEnd(any());

		Injector injector = TestUtils.injector(Map.of(Client.class, client, ItemManager.class, itemManager,
			ClientThread.class, clientThread, HAExporterConfig.class, config));
		messageBuilder = injector.getInstance(MessageBuilder.class);
		killCountTracker = injector.getInstance(KillCountTracker.class);
		collectionNotifier = injector.getInstance(CollectionNotifier.class);
	}

	@Test
	public void testChatOnlySettingUsesTheChatMessage()
	{
		setNotificationSetting(CHAT_ONLY);

		chat("New item added to your collection log: Abyssal whip");

		CollectionData item = onlyCollectionLogEvent();
		assertEquals("Abyssal whip", item.getItemName());
		assertEquals(WHIP_ID, item.getItemId());
		assertEquals(1_500_000L, item.getValue());
	}

	@Test
	public void testPopupSettingIgnoresTheChatMessage()
	{
		setNotificationSetting(POPUP);

		chat("New item added to your collection log: Abyssal whip");

		assertTrue(messageBuilder.getRoot().getEvents().isEmpty());
	}

	@Test
	public void testCollectionLogPopupWithoutNewItemIsIgnored()
	{
		setNotificationSetting(POPUP);

		popup("Collection log", "Something else entirely");

		assertTrue(messageBuilder.getRoot().getEvents().isEmpty());
	}

	@Test
	public void testQueuedPopupIsUsedWithTheKillCountOfItsDrop()
	{
		setNotificationSetting(POPUP);
		chat("Your Vorkath kill count is: 1,250.");
		tick++;
		killCountTracker.onDrop(tick);

		tick += 15; // shown after other popups
		popup("Collection log", "New item:<br><col=ffffff>Abyssal whip</col>");

		CollectionData item = onlyCollectionLogEvent();
		assertEquals("Abyssal whip", item.getItemName());
		assertEquals(Integer.valueOf(1250), item.getKillCount());
	}

	// The whip is worth 1.5M
	@Test
	public void testMinimumValueSkipsCheaperItems()
	{
		setNotificationSetting(CHAT_ONLY);
		when(config.clogMinValue()).thenReturn(2_000_000);
		chat("New item added to your collection log: Abyssal whip");
		assertTrue(messageBuilder.getRoot().getEvents().isEmpty());

		when(config.clogMinValue()).thenReturn(1_000_000);
		chat("New item added to your collection log: Abyssal whip");
		assertEquals("Abyssal whip", onlyCollectionLogEvent().getItemName());
	}

	@Test
	public void testItemWithoutPriceIgnoresMinimumValue()
	{
		setNotificationSetting(CHAT_ONLY);
		when(config.clogMinValue()).thenReturn(2_000_000);

		chat("New item added to your collection log: Pet snakeling");

		assertEquals("Pet snakeling", onlyCollectionLogEvent().getItemName());
	}

	@Test
	public void testParsePopupItemName()
	{
		assertEquals("Abyssal whip", CollectionNotifier.parsePopupItemName("Collection log", "New item:<br>Abyssal whip"));
		assertEquals("Abyssal whip", CollectionNotifier.parsePopupItemName("COLLECTION LOG", "New item: Abyssal whip "));
		assertNull(CollectionNotifier.parsePopupItemName("Combat Task", "New item:<br>Abyssal whip"));
		assertNull(CollectionNotifier.parsePopupItemName("Collection log", "New item:"));
		assertNull(CollectionNotifier.parsePopupItemName("Collection log", null));
		assertNull(CollectionNotifier.parsePopupItemName(null, "New item:<br>Abyssal whip"));
	}

	@Test
	public void testParseKillCount()
	{
		assertEquals(Integer.valueOf(1250), CollectionNotifier.parseKillCount("Your Vorkath kill count is: 1,250."));
		assertEquals(Integer.valueOf(42), CollectionNotifier.parseKillCount("Your completed Chambers of Xeric count is: 42."));
		assertNull(CollectionNotifier.parseKillCount("New item added to your collection log: Abyssal whip"));
	}

	private void setNotificationSetting(int value)
	{
		when(client.getVarbitValue(VarbitID.OPTION_COLLECTION_NEW_ITEM)).thenReturn(value);
	}

	private void chat(String message)
	{
		ChatMessage event = new ChatMessage();
		event.setType(ChatMessageType.GAMEMESSAGE);
		event.setMessage(message);
		collectionNotifier.onChatMessage(event);
	}

	private void popup(String title, String body)
	{
		when(client.getVarcStrValue(VarClientID.NOTIFICATION_TITLE)).thenReturn(title);
		when(client.getVarcStrValue(VarClientID.NOTIFICATION_MAIN)).thenReturn(body);
		collectionNotifier.onScript(new ScriptPreFired(ScriptID.NOTIFICATION_START));
		collectionNotifier.onScript(new ScriptPreFired(ScriptID.NOTIFICATION_DELAY));
	}

	private CollectionData onlyCollectionLogEvent()
	{
		List<Object> events = messageBuilder.getRoot().getEvents();
		assertEquals(1, events.size());
		Map<?, ?> event = (Map<?, ?>) events.get(0);
		assertEquals("collectionLog", event.get("type"));
		return (CollectionData) event.get("data");
	}

	private static ItemComposition item(String name)
	{
		ItemComposition composition = mock(ItemComposition.class);
		when(composition.getName()).thenReturn(name);
		when(composition.getNote()).thenReturn(-1);
		when(composition.getPlaceholderTemplateId()).thenReturn(-1);
		return composition;
	}
}
