package haexporterplugin;

import com.google.gson.Gson;
import haexporterplugin.data.HAConnection;
import haexporterplugin.utils.ConfigUtils;
import haexporterplugin.utils.HomeAssistUtils;
import net.runelite.client.ui.FontManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HAExporterPanelTest
{
	private static final String[] FONT_FILES = {"runescape.ttf", "runescape_small.ttf", "runescape_bold.ttf"};

	private final HAExporterPanel panel = new HAExporterPanel();
	private final List<HAConnection> connections = new ArrayList<>();

	@Before
	public void setUp()
	{
		// Every global toggle on, so only the per-connection toggles decide the status line
		panel.config = mock(HAExporterConfig.class, invocation ->
			invocation.getMethod().getReturnType() == boolean.class
				? Boolean.TRUE
				: RETURNS_DEFAULTS.answer(invocation));
		panel.homeAssistUtils = mock(HomeAssistUtils.class);
		panel.configUtils = mock(ConfigUtils.class);
		panel.gson = new Gson();

		when(panel.configUtils.getStoredConnections()).thenReturn(connections);
	}

	@After
	public void tearDown()
	{
		panel.onDeactivate();
	}

	// The side panel is drawn in RuneLite's RuneScape font. macOS has no fallback font for it,
	// so any character the font lacks shows up there as a "missing glyph" box.
	@Test
	public void testHomeViewOnlyUsesCharactersInTheRuneScapeFont() throws Exception
	{
		connections.add(healthyConnection());
		connections.add(pausedConnection());
		connections.add(connectionWithTogglesOff());
		connections.add(disabledConnection());

		panel.initialize();

		assertFalse(textsIn(panel).isEmpty());
		assertAllDisplayable(textsIn(panel));
	}

	@Test
	public void testSettingsViewOnlyUsesCharactersInTheRuneScapeFont() throws Exception
	{
		connections.add(disabledConnection());
		panel.initialize();

		settingsButtons().get(0).doClick();

		assertTrue(textsIn(panel).contains("Connection Settings"));
		assertAllDisplayable(textsIn(panel));
	}

	@Test
	public void testSettingsButtonShowsAnIcon()
	{
		connections.add(healthyConnection());
		panel.initialize();

		assertEquals(1, settingsButtons().size());
		assertNotNull(settingsButtons().get(0).getIcon());
	}

	@Test
	public void testStatusLinesShowAnIcon()
	{
		connections.add(healthyConnection());
		connections.add(pausedConnection());
		connections.add(connectionWithTogglesOff());
		connections.add(disabledConnection());

		panel.initialize();

		assertNotNull(labelContaining("All enabled").getIcon());
		assertNotNull(labelContaining("Disabled: Inv, Loot").getIcon());
		assertNotNull(labelContaining("Paused").getIcon());
		assertNotNull(labelContaining("Unauthorized").getIcon());
	}

	// A disabled connection sends nothing, so its per-toggle status would only contradict the warning
	@Test
	public void testDisabledConnectionHidesToggleStatus()
	{
		HAConnection disabled = disabledConnection();
		HAConnection disabledWithTogglesOff = connectionWithTogglesOff();
		disabledWithTogglesOff.setEnabled(false);
		connections.add(disabled);
		connections.add(disabledWithTogglesOff);

		panel.initialize();

		assertNotNull(labelContaining("Unauthorized"));
		assertNull(findLabel("All enabled"));
		assertNull(findLabel("Disabled:"));
	}

	private static HAConnection healthyConnection()
	{
		HAConnection connection = new HAConnection("https://ha.example", "token-healthy");
		connection.setFriendlyName("Main HA");
		return connection;
	}

	private HAConnection pausedConnection()
	{
		HAConnection connection = new HAConnection("https://paused.example", "token-paused");
		when(panel.homeAssistUtils.getPausedUntil(connection)).thenReturn(System.currentTimeMillis() + 90_000L);
		when(panel.homeAssistUtils.getQueuedCount(connection)).thenReturn(2);
		return connection;
	}

	private static HAConnection connectionWithTogglesOff()
	{
		HAConnection connection = new HAConnection("https://partial.example", "token-partial");
		connection.setIncludeInventory(false);
		connection.setIncludeLootEvents(false);
		return connection;
	}

	private static HAConnection disabledConnection()
	{
		HAConnection connection = new HAConnection("https://disabled.example", "token-disabled");
		connection.setEnabled(false);
		connection.setDisabledReason("Unauthorized (401)");
		return connection;
	}

	private List<AbstractButton> settingsButtons()
	{
		List<AbstractButton> buttons = new ArrayList<>();
		for (Component component : componentsIn(panel))
		{
			if (component instanceof AbstractButton
				&& "Settings".equals(((AbstractButton) component).getToolTipText()))
			{
				buttons.add((AbstractButton) component);
			}
		}
		return buttons;
	}

	private JLabel labelContaining(String fragment)
	{
		JLabel label = findLabel(fragment);
		assertNotNull("No label containing \"" + fragment + "\" in " + textsIn(panel), label);
		return label;
	}

	// The first visible label whose text contains the fragment, or null
	private JLabel findLabel(String fragment)
	{
		for (Component component : componentsIn(panel))
		{
			if (component instanceof JLabel && component.isVisible())
			{
				String text = ((JLabel) component).getText();
				if (text != null && text.contains(fragment))
				{
					return (JLabel) component;
				}
			}
		}
		return null;
	}

	private static void assertAllDisplayable(List<String> texts) throws Exception
	{
		for (String fontFile : FONT_FILES)
		{
			// Loaded straight from the file: FontManager's own fonts gain a system fallback on
			// Windows and Linux, which would hide the missing glyphs this is looking for
			Font font;
			try (InputStream in = FontManager.class.getResourceAsStream(fontFile))
			{
				font = Font.createFont(Font.TRUETYPE_FONT, in);
			}

			for (String text : texts)
			{
				int index = font.canDisplayUpTo(text);
				if (index != -1)
				{
					fail(String.format("U+%04X in \"%s\" is not in %s", (int) text.charAt(index), text, fontFile));
				}
			}
		}
	}

	private static List<String> textsIn(Container root)
	{
		List<String> texts = new ArrayList<>();
		for (Component component : componentsIn(root))
		{
			String text = null;
			if (component instanceof JLabel)
			{
				text = ((JLabel) component).getText();
			}
			else if (component instanceof AbstractButton)
			{
				text = ((AbstractButton) component).getText();
			}

			if (text != null && !text.isEmpty())
			{
				texts.add(text);
			}
		}
		return texts;
	}

	private static List<Component> componentsIn(Container root)
	{
		List<Component> components = new ArrayList<>();
		for (Component child : root.getComponents())
		{
			components.add(child);
			if (child instanceof Container)
			{
				components.addAll(componentsIn((Container) child));
			}
		}
		return components;
	}
}
