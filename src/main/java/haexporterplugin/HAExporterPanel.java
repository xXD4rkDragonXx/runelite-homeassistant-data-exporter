package haexporterplugin;

import com.google.gson.Gson;
import haexporterplugin.data.HAConnection;
import haexporterplugin.data.TokenCallback;
import haexporterplugin.utils.ConfigUtils;
import haexporterplugin.utils.HomeAssistUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.List;

@Slf4j
public class HAExporterPanel extends PluginPanel
{
    protected @Inject HAExporterConfig config;
    protected @Inject HomeAssistUtils homeAssistUtils;
    protected @Inject ConfigUtils configUtils;

    private final JPanel mainPanel = new JPanel(new BorderLayout());

    // Placeholder stats labels (update from singleton later)
    private final JLabel goldLabel = new JLabel("Gold Stored: 0");
    private final JLabel itemsLabel = new JLabel("Items Processed: 0");

    private final JLabel creditLabel = new JLabel("A plugin by: ");
    private final JLabel authorLabel = new JLabel("xXD4rkDragonXx & RedFireBreak");

    public final int CODE_LENGTH = 5;

    public HAExporterPanel()
    {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
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

        JPanel container = new JPanel(new BorderLayout());

        // Will be expanded on in a future release
        // container.add(buildStatsPanel(), BorderLayout.NORTH);

        container.add(buildCreditPanel(), BorderLayout.NORTH);



        JPanel centerPanel = new JPanel();
        centerPanel.add(Box.createVerticalStrut(15));
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        JButton connectButton = new JButton("Connect New Device");
        connectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        connectButton.addActionListener(e -> showConnectionCodeInput());

        centerPanel.add(connectButton);
        centerPanel.add(Box.createVerticalStrut(15));

        centerPanel.add(buildConnectionsPanel());

        container.add(centerPanel, BorderLayout.CENTER);

        mainPanel.add(container, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel buildConnectionsPanel()
    {
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(BorderFactory.createTitledBorder("Connected Devices"));

        List<HAConnection> connections = configUtils.getStoredConnections();
        log.debug(Integer.toString(connections.size()));

        if (connections.isEmpty())
        {
            JLabel none = new JLabel("No devices connected.");
            none.setAlignmentX(Component.CENTER_ALIGNMENT);
            wrapper.add(none);
            return wrapper;
        }

        for (HAConnection connection : connections)
        {
            JPanel row = new JPanel(new BorderLayout());
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            log.debug("{} {}", connection.baseUrl, connection.token);
            log.debug(String.valueOf(connections.indexOf(connection)));

            JLabel label = new JLabel(connection.getBaseUrl());
            JButton removeButton = new JButton("Remove");

            removeButton.addActionListener(e ->
                    removeConnection(connection));

            row.add(label, BorderLayout.CENTER);
            row.add(removeButton, BorderLayout.EAST);

            wrapper.add(row);
            wrapper.add(Box.createVerticalStrut(5));
        }

        return wrapper;
    }

    private void removeConnection(HAConnection connection)
    {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Remove this device?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );

        if (result != JOptionPane.YES_OPTION)
            return;

        List<HAConnection> connections = configUtils.getStoredConnections();
        connections.removeIf(c ->
                c.getBaseUrl().equals(connection.getBaseUrl())
                        && c.getToken().equals(connection.getToken())
        );

        Gson gson = new Gson();
        config.setHomeassistantConnections(gson.toJson(connections));

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

        baseUrlField.addKeyListener(new java.awt.event.KeyAdapter()
        {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e)
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

        // Base URL label + input
        JLabel urlLabel = new JLabel("Base URL:");
        urlLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        topPanel.add(urlLabel);
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
            String baseUrl = baseUrlField.getText().trim();

            StringBuilder code = new StringBuilder();
            for (JTextField field : fields)
            {
                code.append(field.getText());
            }

            submitButton.setEnabled(false); // prevent double click

            homeAssistUtils.getToken(baseUrl, code.toString(), new TokenCallback()
            {
                @Override
                public void onSuccess(String token)
                {
                    SwingUtilities.invokeLater(() ->
                    {
                        handleSuccessfulConnection(baseUrl, token);
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
                                "Connection failed, try again.\n" + e.getMessage(),
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

    private void handleSuccessfulConnection(String baseUrl, String token)
    {
        configUtils.addStoredConnection(baseUrl, token);

        JOptionPane.showMessageDialog(
                this,
                "Connection saved!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );

        showHomeView();
    }
}