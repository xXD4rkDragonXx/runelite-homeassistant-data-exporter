package haexporterplugin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import haexporterplugin.data.HAConnection;
import haexporterplugin.data.TokenCallback;
import haexporterplugin.utils.HomeAssistUtils;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;


public class HAExporterPanel extends PluginPanel
{
    protected @Inject HAExporterConfig config;
    protected @Inject HomeAssistUtils homeAssistUtils;

    private final JPanel mainPanel = new JPanel(new BorderLayout());

    // Placeholder stats labels (update from singleton later)
    private final JLabel goldLabel = new JLabel("Gold Stored: 0");
    private final JLabel itemsLabel = new JLabel("Items Processed: 0");

    public final int CODE_LENGTH = 5;

    public HAExporterPanel()
    {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        showHomeView();
    }

    /* ============================
       HOME VIEW
       ============================ */

    private void showHomeView()
    {
        mainPanel.removeAll();

        JPanel container = new JPanel();
        container.setLayout(new BorderLayout());

        container.add(buildStatsPanel(), BorderLayout.NORTH);

        JButton connectButton = new JButton("Connect New Device");
        connectButton.addActionListener(e -> showConnectionCodeInput());

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.add(connectButton);

        // TODO: Show Connected devices, gotten from the config
        // TODO: Add Remove button to all devices

        container.add(centerPanel, BorderLayout.CENTER);

        mainPanel.add(container, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel buildStatsPanel()
    {
        JPanel statsPanel = new JPanel();
        statsPanel.setLayout(new GridLayout(2, 1));
        statsPanel.setBorder(BorderFactory.createTitledBorder("Stats"));

        statsPanel.add(goldLabel);
        statsPanel.add(itemsLabel);

        return statsPanel;
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

                        if (data == null) return false;
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
        Gson gson = new Gson();
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

        connections.add(new HAConnection(baseUrl, token));
        config.setHomeassistantConnections(gson.toJson(connections));

        JOptionPane.showMessageDialog(
                this,
                "Connection saved!",
                "Success",
                JOptionPane.INFORMATION_MESSAGE
        );

        showHomeView();
    }
}