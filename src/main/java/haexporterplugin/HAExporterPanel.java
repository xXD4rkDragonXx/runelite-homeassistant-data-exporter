package haexporterplugin;

import com.google.gson.Gson;
import haexporterplugin.data.HAConnection;
import haexporterplugin.data.PairingException;
import haexporterplugin.data.TokenCallback;
import haexporterplugin.utils.ConfigUtils;
import haexporterplugin.utils.HomeAssistUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ImageUtil;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HAExporterPanel extends PluginPanel
{
    protected @Inject HAExporterConfig config;
    protected @Inject HomeAssistUtils homeAssistUtils;
    protected @Inject ConfigUtils configUtils;
    protected @Inject Gson gson;

    private final JPanel mainPanel = new JPanel(new BorderLayout());

    // Placeholder stats labels (update from singleton later)
    private final JLabel goldLabel = new JLabel("Gold Stored: 0");
    private final JLabel itemsLabel = new JLabel("Items Processed: 0");

    private final JLabel creditLabel = new JLabel("A plugin by: ");
    private final JLabel authorLabel = new JLabel("xXD4rkDragonXx & RedFireBreak");

    public final int CODE_LENGTH = 5;

    // Bundled images rather than Unicode symbols: the RuneScape font has no glyphs for those,
    // and macOS has no fallback font to borrow them from
    private static final ImageIcon SETTINGS_ICON = loadIcon("settings");
    private static final ImageIcon WARNING_ICON = loadIcon("warning");
    private static final ImageIcon PAUSE_ICON = loadIcon("pause");
    private static final ImageIcon CHECK_ICON = loadIcon("check");
    private static final ImageIcon CROSS_ICON = loadIcon("cross");

    // Pause indicators on the home view, refreshed every second while it is shown
    private final Map<JLabel, HAConnection> pauseLabels = new HashMap<>();
    private final Timer pauseTimer = new Timer(1000, e -> updatePauseLabels());

    public HAExporterPanel()
    {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    @Override
    public void onActivate()
    {
        super.onActivate();
        showHomeView(); // Rebuild the UI every time the panel is opened
    }

    @Override
    public void onDeactivate()
    {
        super.onDeactivate();
        pauseTimer.stop();
    }

    public void initialize()
    {
        showHomeView();
    }


    /* ============================
       HOME VIEW
       ============================ */

    private void showHomeView()
    {
        mainPanel.removeAll();

        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Will be expanded on in a future release
        // container.add(buildStatsPanel());

        JPanel creditPanelWrapper = new JPanel();
        creditPanelWrapper.setLayout(new BoxLayout(creditPanelWrapper, BoxLayout.X_AXIS));
        creditPanelWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        creditPanelWrapper.add(Box.createHorizontalGlue());
        JPanel creditPanel = buildCreditPanel();
        creditPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        creditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, creditPanel.getPreferredSize().height));
        creditPanelWrapper.add(creditPanel);
        creditPanelWrapper.add(Box.createHorizontalGlue());
        container.add(creditPanelWrapper);

        container.add(Box.createVerticalStrut(15));

        JButton connectButton = new JButton("Connect New Device");
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectButton.setMaximumSize(new Dimension(200, connectButton.getPreferredSize().height));
        connectButton.addActionListener(e -> showConnectionCodeInput());

        container.add(connectButton);
        container.add(Box.createVerticalStrut(15));

        JPanel connectionsPanel = buildConnectionsPanel();
        connectionsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectionsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        container.add(connectionsPanel);

        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.add(container, BorderLayout.CENTER);
        mainPanel.add(centerWrapper, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel buildConnectionsPanel()
    {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(BorderFactory.createTitledBorder("Connected Devices"));

        pauseTimer.stop();
        pauseLabels.clear();

        List<HAConnection> connections = configUtils.getStoredConnections();

        if (connections.isEmpty())
        {
            JLabel none = new JLabel("No devices connected.");
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            wrapper.add(none);
            return wrapper;
        }

        for (int i = 0; i < connections.size(); i++)
        {
            HAConnection connection = connections.get(i);

            JPanel card = new JPanel();
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);

            // Header panel with name and settings button
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel nameLabel = new JLabel(connection.getDisplayName());
            headerPanel.add(nameLabel, BorderLayout.WEST);

            JButton settingsButton = new JButton(SETTINGS_ICON);
            settingsButton.setPreferredSize(new Dimension(30, 20));
            settingsButton.setMargin(new Insets(0, 0, 0, 0));
            settingsButton.setToolTipText("Settings");
            settingsButton.addActionListener(e -> showConnectionSettings(connection));
            headerPanel.add(settingsButton, BorderLayout.EAST);

            card.add(headerPanel);

            if (connection.isEnabled())
            {
                // Pause indicator, hidden while the connection is delivering normally
                JLabel pauseLabel = new JLabel(PAUSE_ICON, SwingConstants.LEADING);
                pauseLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
                pauseLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                updatePauseLabel(pauseLabel, connection);
                card.add(pauseLabel);
                pauseLabels.put(pauseLabel, connection);

                card.add(Box.createVerticalStrut(3));

                JLabel statusLabel = buildStatusLabel(connection);
                statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(statusLabel);
            }
            else
            {
                // A disabled connection sends nothing, so the warning replaces the toggle status
                card.add(Box.createVerticalStrut(3));
                String reason = connection.getDisabledReason() != null
                        ? connection.getDisabledReason()
                        : "Disabled";
                JLabel disabledLabel = new JLabel(
                        "<html><span style='color:orange;'>" + reason + "</span></html>",
                        WARNING_ICON,
                        SwingConstants.LEADING
                );
                disabledLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                card.add(disabledLabel);
            }

            wrapper.add(card);

            if (i < connections.size() - 1)
            {
                wrapper.add(Box.createVerticalStrut(5));
                JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
                sep.setAlignmentX(Component.LEFT_ALIGNMENT);
                sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                wrapper.add(sep);
                wrapper.add(Box.createVerticalStrut(5));
            }
        }

        if (!pauseLabels.isEmpty())
        {
            pauseTimer.restart();
        }

        return wrapper;
    }

    private void updatePauseLabels()
    {
        boolean homeViewShowing = false;
        for (Map.Entry<JLabel, HAConnection> entry : pauseLabels.entrySet())
        {
            Container card = entry.getKey().getParent();
            if (card != null && card.isShowing())
            {
                homeViewShowing = true;
                updatePauseLabel(entry.getKey(), entry.getValue());
            }
        }

        if (!homeViewShowing)
        {
            // Switched to the settings or pairing view, or the panel is hidden
            pauseTimer.stop();
        }
    }

    private void updatePauseLabel(JLabel label, HAConnection connection)
    {
        long remaining = homeAssistUtils.getPausedUntil(connection) - System.currentTimeMillis();
        if (remaining <= 0)
        {
            label.setVisible(false);
            return;
        }

        int queued = homeAssistUtils.getQueuedCount(connection);
        String text = "Paused \u2014 retrying in " + formatRemaining(remaining)
                + (queued > 0 ? " (" + queued + " queued)" : "");
        label.setText("<html><span style='color:orange;'>" + text + "</span></html>");
        label.setVisible(true);
    }

    private static String formatRemaining(long millis)
    {
        long seconds = (millis + 999) / 1000;
        return seconds < 60
                ? seconds + "s"
                : String.format("%dm %02ds", seconds / 60, seconds % 60);
    }

    private JLabel buildStatusLabel(HAConnection connection)
    {
        List<String> disabled = new ArrayList<>();

        if (!connection.isIncludeInventory() || !config.includeInventory()) disabled.add("Inv");
        if (!connection.isIncludeEquipment() || !config.includeEquipment()) disabled.add("Equip");
        if (!connection.isIncludeLocation() || !config.includeLocation()) disabled.add("Loc");
        if (!connection.isIncludeLootEvents() || !config.includeLootEvents()) disabled.add("Loot");
        if (!connection.isIncludeDeathEvents() || !config.includeDeathEvents()) disabled.add("Death");
        if (!connection.isIncludeLevelUpEvents() || !config.includeLevelUpEvents()) disabled.add("Lvl");
        if (!connection.isIncludeAchievementDiaryEvents() || !config.includeAchievementDiaryEvents()) disabled.add("Diary");
        if (!connection.isIncludeCombatTaskEvents() || !config.includeCombatTaskEvents()) disabled.add("Task");
        if (!connection.isIncludeSuperiorEvents() || !config.includeSuperiorEvents()) disabled.add("Superior");
        if (!connection.isIncludeCollectionLogEvents() || !config.includeCollectionLogEvents()) disabled.add("CollLog");

        if (disabled.isEmpty())
        {
            return new JLabel("All enabled", CHECK_ICON, SwingConstants.LEADING);
        }

        return new JLabel("<html>Disabled: " + String.join(", ", disabled) + "</html>", CROSS_ICON, SwingConstants.LEADING);
    }

    private void removeConnection(HAConnection connection)
    {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to remove \"" + connection.getDisplayName() + "\"?\n\nThis action cannot be undone.",
                "Remove Device - Confirm",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (result != JOptionPane.YES_OPTION)
            return;

        List<HAConnection> connections = configUtils.getStoredConnections();
        connections.removeIf(c ->
                c.getBaseUrl().equals(connection.getBaseUrl())
                        && c.getToken().equals(connection.getToken())
        );

        config.setHomeassistantConnections(gson.toJson(connections));

        JOptionPane.showMessageDialog(
                this,
                "Device removed successfully.",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );

        showHomeView(); // refresh UI
    }


    // Boilerplate for future use, will not be used in the current version
    private JPanel buildStatsPanel()
    {
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(2, 1));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Stats"));

        statsPanel.add(goldLabel);
        statsPanel.add(itemsLabel);

        return statsPanel;
    }

    private JPanel buildCreditPanel()
    {
        JPanel creditPanel = new JPanel();
        creditPanel.setLayout(new GridLayout(2, 1));
        creditPanel.setBorder(BorderFactory.createTitledBorder("Credit"));

        creditPanel.add(creditLabel);
        creditPanel.add(authorLabel);

        return creditPanel;
    }

    /* ============================
       CONNECTION SETTINGS VIEW
       ============================ */

    private void showConnectionSettings(HAConnection connection)
    {
        mainPanel.removeAll();

        JPanel container = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Connection Settings", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(title);
        topPanel.add(Box.createVerticalStrut(5));

        JLabel urlLabel = new JLabel(connection.getBaseUrl());
        urlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(urlLabel);
        topPanel.add(Box.createVerticalStrut(15));

        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setBorder(BorderFactory.createTitledBorder("Friendly Name"));

        JTextField nameField = new JTextField(connection.getFriendlyName() != null ? connection.getFriendlyName() : "");
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        namePanel.add(nameField);

        topPanel.add(namePanel);
        topPanel.add(Box.createVerticalStrut(10));

        JPanel dataTogglesPanel = new JPanel();
        dataTogglesPanel.setLayout(new BoxLayout(dataTogglesPanel, BoxLayout.Y_AXIS));
        dataTogglesPanel.setBorder(BorderFactory.createTitledBorder("Data Toggles"));
        dataTogglesPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
        statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox enabledCheckbox = new JCheckBox("Enabled", connection.isEnabled());
        enabledCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusPanel.add(enabledCheckbox);

        int minStatusWidth = namePanel.getPreferredSize().width;
        Dimension statusPref = statusPanel.getPreferredSize();
        statusPanel.setPreferredSize(new Dimension(Math.max(statusPref.width, minStatusWidth), statusPref.height));

        topPanel.add(statusPanel);

        if (!connection.isEnabled() && connection.getDisabledReason() != null)
        {
            JLabel warningLabel = new JLabel(WARNING_ICON, SwingConstants.LEADING);
            int warningWidth = Math.max(minStatusWidth - 16, 180)
                    - WARNING_ICON.getIconWidth() - warningLabel.getIconTextGap();
            warningLabel.setText(
                    "<html><div style='width:" + warningWidth + "px;color:orange;'>"
                            + connection.getDisabledReason() +
                            "</div></html>"
            );
            warningLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            topPanel.add(Box.createVerticalStrut(6));
            topPanel.add(warningLabel);
            topPanel.add(Box.createVerticalStrut(10));
        }
        else
        {
            topPanel.add(Box.createVerticalStrut(10));
        }

        JPanel togglesPanel = new JPanel();
        togglesPanel.setLayout(new BoxLayout(togglesPanel, BoxLayout.Y_AXIS));
        togglesPanel.setBorder(BorderFactory.createTitledBorder("Data Toggles"));
        togglesPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox inventoryCheckbox = createCheckbox(
                "Include Inventory",
                connection.isIncludeInventory(),
                config.includeInventory()
        );

        JCheckBox equipmentCheckbox = createCheckbox(
                "Include Equipment",
                connection.isIncludeEquipment(),
                config.includeEquipment()
        );

        JCheckBox locationCheckbox = createCheckbox(
                "Include Location",
                connection.isIncludeLocation(),
                config.includeLocation()
        );

        dataTogglesPanel.add(inventoryCheckbox);
        dataTogglesPanel.add(equipmentCheckbox);
        dataTogglesPanel.add(locationCheckbox);

        topPanel.add(dataTogglesPanel);
        topPanel.add(Box.createVerticalStrut(10));

        JPanel eventTogglesPanel = new JPanel();
        eventTogglesPanel.setLayout(new BoxLayout(eventTogglesPanel, BoxLayout.Y_AXIS));
        eventTogglesPanel.setBorder(BorderFactory.createTitledBorder("Event Toggles"));
        eventTogglesPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox lootCheckbox = createCheckbox("Loot Events", connection.isIncludeLootEvents(), config.includeLootEvents());
        JCheckBox deathCheckbox = createCheckbox("Death Events", connection.isIncludeDeathEvents(), config.includeDeathEvents());
        JCheckBox levelUpCheckbox = createCheckbox("Level-Up Events", connection.isIncludeLevelUpEvents(), config.includeLevelUpEvents());
        JCheckBox diaryCheckbox = createCheckbox("Achievement Diary Events", connection.isIncludeAchievementDiaryEvents(), config.includeAchievementDiaryEvents());
        JCheckBox combatTaskCheckbox = createCheckbox("Combat Task Events", connection.isIncludeCombatTaskEvents(), config.includeCombatTaskEvents());
        JCheckBox superiorCheckbox = createCheckbox("Superior Spawn Events", connection.isIncludeSuperiorEvents(), config.includeSuperiorEvents());
        JCheckBox collectionLogCheckbox = createCheckbox("Collection Log Events", connection.isIncludeCollectionLogEvents(), config.includeCollectionLogEvents());

        eventTogglesPanel.add(lootCheckbox);
        eventTogglesPanel.add(deathCheckbox);
        eventTogglesPanel.add(levelUpCheckbox);
        eventTogglesPanel.add(diaryCheckbox);
        eventTogglesPanel.add(combatTaskCheckbox);
        eventTogglesPanel.add(superiorCheckbox);
        eventTogglesPanel.add(collectionLogCheckbox);

        topPanel.add(eventTogglesPanel);
        topPanel.add(Box.createVerticalStrut(15));

        JButton removeButton = new JButton("Remove Device");
        removeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeButton.setMaximumSize(new Dimension(200, removeButton.getPreferredSize().height));
        removeButton.setForeground(Color.RED);
        removeButton.addActionListener(e -> removeConnection(connection));
        topPanel.add(removeButton);

        container.add(topPanel, BorderLayout.CENTER);

        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));
        buttonContainer.add(Box.createVerticalStrut(5));

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> showHomeView());

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e ->
        {
            List<HAConnection> connections = configUtils.getStoredConnections();
            for (HAConnection c : connections)
            {
                if (c.getBaseUrl().equals(connection.getBaseUrl())
                        && c.getToken().equals(connection.getToken()))
                {
                    String name = nameField.getText().trim();
                    c.setFriendlyName(name.isEmpty() ? null : name);
                    c.setEnabled(enabledCheckbox.isSelected());
                    if (enabledCheckbox.isSelected())
                    {
                        c.setDisabledReason(null);
                    }
                    c.setIncludeInventory(inventoryCheckbox.isSelected());
                    c.setIncludeEquipment(equipmentCheckbox.isSelected());
                    c.setIncludeLocation(locationCheckbox.isSelected());
                    c.setIncludeLootEvents(lootCheckbox.isSelected());
                    c.setIncludeDeathEvents(deathCheckbox.isSelected());
                    c.setIncludeLevelUpEvents(levelUpCheckbox.isSelected());
                    c.setIncludeAchievementDiaryEvents(diaryCheckbox.isSelected());
                    c.setIncludeCombatTaskEvents(combatTaskCheckbox.isSelected());
                    c.setIncludeSuperiorEvents(superiorCheckbox.isSelected());
                    c.setIncludeCollectionLogEvents(collectionLogCheckbox.isSelected());
                }
            }
            config.setHomeassistantConnections(gson.toJson(connections));

            JOptionPane.showMessageDialog(
                    HAExporterPanel.this,
                    "Settings saved!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
            );

            showHomeView();
        });

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        buttonPanel.add(backButton);
        buttonPanel.add(saveButton);

        buttonContainer.add(buttonPanel);

        container.add(buttonContainer, BorderLayout.SOUTH);

        mainPanel.add(container, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    /* ============================
       CONNECTION CODE VIEW
       ============================ */

    private void showConnectionCodeInput()
    {
        mainPanel.removeAll();

        JPanel container = new JPanel(new BorderLayout());

        // -----------------------------
        // STACKED TOP PANEL: Title + Code + Base URL
        // -----------------------------
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // BaseURL input
        JTextField baseUrlField = new JTextField();
        baseUrlField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        // Title
        JLabel title = new JLabel("Enter " + CODE_LENGTH + "-Digit Connection Code", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(title);

        topPanel.add(Box.createVerticalStrut(10)); // spacing

        // Code input fields
        JPanel codePanel = new JPanel(new GridLayout(1, CODE_LENGTH, 5, 0));
        JTextField[] fields = new JTextField[CODE_LENGTH];

        JButton submitButton = new JButton("Submit");
        submitButton.setEnabled(false);

        // Completion checker
        Runnable updateSubmitState = () ->
        {
            for (JTextField field : fields)
            {
                if (field == null || field.getText().isEmpty() || baseUrlField.getText().trim().isEmpty())
                {
                    submitButton.setEnabled(false);
                    return;
                }
            }
            submitButton.setEnabled(true);
        };

        baseUrlField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener()
        {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e)
            {
                updateSubmitState.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e)
            {
                updateSubmitState.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e)
            {
                updateSubmitState.run();
            }
        });

        for (int i = 0; i < CODE_LENGTH; i++)
        {
            JTextField field = new JTextField();
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setPreferredSize(new Dimension(40, 40));

            final int index = i;

            field.addKeyListener(new java.awt.event.KeyAdapter()
            {
                @Override
                public void keyTyped(java.awt.event.KeyEvent e)
                {
                    char c = e.getKeyChar();
                    if (!Character.isDigit(c))
                    {
                        e.consume();
                        return;
                    }

                    field.setText(String.valueOf(c));
                    e.consume();

                    if (index < CODE_LENGTH - 1)
                    {
                        fields[index + 1].requestFocus();
                    }

                    updateSubmitState.run();
                }

                @Override
                public void keyPressed(java.awt.event.KeyEvent e)
                {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_BACK_SPACE)
                    {
                        if (field.getText().isEmpty() && index > 0)
                        {
                            fields[index - 1].setText("");
                            fields[index - 1].requestFocus();
                        }
                        else
                        {
                            field.setText("");
                        }

                        updateSubmitState.run();
                        e.consume();
                    }
                }
            });

            // Paste handling
            field.setTransferHandler(new TransferHandler()
            {
                @Override
                public boolean importData(TransferSupport support)
                {
                    try
                    {
                        String data = (String) support.getTransferable()
                                .getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);

                        data = data.replaceAll("\\D", "");
                        if (data.isEmpty()) return false;

                        for (int j = 0; j < CODE_LENGTH; j++)
                        {
                            if (j < data.length())
                                fields[j].setText(String.valueOf(data.charAt(j)));
                            else
                                fields[j].setText("");
                        }

                        if (data.length() >= CODE_LENGTH)
                            fields[CODE_LENGTH - 1].requestFocus();
                        else
                            fields[data.length()].requestFocus();

                        updateSubmitState.run();
                        return true;
                    }
                    catch (Exception ex)
                    {
                        return false;
                    }
                }
            });

            fields[i] = field;
            codePanel.add(field);
        }

        codePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(codePanel);

        topPanel.add(Box.createVerticalStrut(15)); // spacing

        // Endpoint URL label + helper text + input
        JLabel urlLabel = new JLabel("Endpoint URL");
        urlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(urlLabel);

        JLabel urlHelpLabel = new JLabel("Home Assistant or any compatible endpoint");
        urlHelpLabel.setFont(FontManager.getRunescapeSmallFont());
        urlHelpLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        urlHelpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(urlHelpLabel);
        topPanel.add(Box.createVerticalStrut(3));

        topPanel.add(baseUrlField);

        container.add(topPanel, BorderLayout.CENTER);

        // -----------------------------
        // Bottom buttons: Back + Submit
        // -----------------------------

        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new BoxLayout(buttonContainer, BoxLayout.Y_AXIS));

        // Add spacing between Base URL and buttons
        buttonContainer.add(Box.createVerticalStrut(5));

        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> showHomeView());

        submitButton.addActionListener(e ->
        {
            String baseUrl = normalizeBaseUrl(baseUrlField.getText().trim());

            StringBuilder code = new StringBuilder();
            for (JTextField field : fields)
            {
                code.append(field.getText());
            }

            submitButton.setEnabled(false); // prevent double click

            homeAssistUtils.getToken(baseUrl, code.toString(), new TokenCallback()
            {
                @Override
                public void onSuccess(String token, String name)
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        handleSuccessfulConnection(baseUrl, token, name);
                        submitButton.setEnabled(true);
                    });
                }

                @Override
                public void onFailure(Exception e)
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        submitButton.setEnabled(true);
                        JOptionPane.showMessageDialog(
                                HAExporterPanel.this,
                                buildPairingFailureMessage(e),
                                "Failure",
                                JOptionPane.ERROR_MESSAGE
                        );
                    });
                }
            });
        });


        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        buttonPanel.add(backButton);
        buttonPanel.add(submitButton);

        buttonContainer.add(buttonPanel);

        container.add(buttonContainer, BorderLayout.SOUTH);

        mainPanel.add(container, BorderLayout.CENTER);

        fields[0].requestFocus();
        revalidate();
        repaint();
    }

    private void handleSuccessfulConnection(String baseUrl, String token, String name)
    {
        configUtils.addStoredConnection(baseUrl, token, name);

        JOptionPane.showMessageDialog(
                this,
                "Connection saved!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );

        showHomeView();
    }

    private static String buildPairingFailureMessage(Exception e)
    {
        // Prefer the endpoint's own (sanitized) explanation, e.g. "Code expired"
        if (e instanceof PairingException && ((PairingException) e).getServerMessage() != null)
        {
            return ((PairingException) e).getServerMessage();
        }

        return "Connection failed, try again.\n" + e.getMessage();
    }

    private JCheckBox createCheckbox(String label, boolean selected, boolean globallyEnabled) {
        JCheckBox checkBox = new JCheckBox(
                globallyEnabled ? label : label + " (Globally disabled)",
                globallyEnabled && selected
        );

        checkBox.setEnabled(globallyEnabled);

        if (!globallyEnabled) {
            checkBox.setToolTipText("Turned off in the HA Exporter config tab");
        }

        checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        return checkBox;
    }

    private static ImageIcon loadIcon(String name)
    {
        return new ImageIcon(ImageUtil.loadImageResource(HAExporterPanel.class, "/icons/" + name + ".png"));
    }

    private static String normalizeBaseUrl(String url)
    {
        return url.trim().replaceAll("/+$", "");
    }
}

