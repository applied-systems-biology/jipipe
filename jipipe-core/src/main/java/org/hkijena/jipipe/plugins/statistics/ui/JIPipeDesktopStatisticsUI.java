package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.service.components.JIPipeStatisticsServiceComponent;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWorkbench;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItem;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsItemCategory;
import org.hkijena.jipipe.plugins.statistics.StatisticsReporter;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class JIPipeDesktopStatisticsUI extends JDialog {
    private final JIPipeDesktopProjectWorkbench workbench;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public JIPipeDesktopStatisticsUI(JIPipeDesktopProjectWorkbench workbench) {
        super(workbench.getWindow(), "Statistics");
        this.workbench = workbench;
        setModal(true);
        initialize();
        pack();
        setSize(700, 500);
        setLocationRelativeTo(workbench.getWindow());
        UIUtils.addEscapeListener(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JIPipeStatisticsServiceComponent service = JIPipe.getInstance().getStatistics();
        JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();

        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        topPanel.add(new JLabel("Machine ID:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        JTextField machineIdField = new JTextField(service.getMachineId());
        machineIdField.setEditable(false);
        topPanel.add(machineIdField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        JButton rerollButton = UIUtils.createButton("Re-roll", JIPipe.RESOURCES.getIcon16("actions/reload.png"), () -> {
            service.rerollMachineId();
            machineIdField.setText(service.getMachineId());
        });
        topPanel.add(rerollButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        topPanel.add(new JLabel("First launch:"), gbc);
        gbc.gridx = 1;
        LocalDateTime firstLaunch = service.getFirstLaunchTimestamp();
        topPanel.add(new JLabel(firstLaunch != null ? firstLaunch.format(FORMATTER) : "Unknown"), gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        topPanel.add(new JLabel("Last sent:"), gbc);
        gbc.gridx = 1;
        LocalDateTime lastSent = service.getLastSentTimestamp();
        topPanel.add(new JLabel(lastSent != null ? lastSent.format(FORMATTER) : "Never"), gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        topPanel.add(new JLabel("Privacy level:"), gbc);
        gbc.gridx = 1;
        topPanel.add(new JLabel(settings.getPrivacyLevel().name() + " - " + settings.getPrivacyLevel().getDescription()), gbc);

        add(topPanel, BorderLayout.NORTH);

        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(UIManager.getColor("Panel.background"));

        Map<JIPipeStatisticsItemCategory, List<JIPipeStatisticsItem>> grouped = service.getRegistry().getItemsByCategory();
        for (JIPipeStatisticsItemCategory category : JIPipeStatisticsItemCategory.values()) {
            List<JIPipeStatisticsItem> items = grouped.get(category);
            if (items == null || items.isEmpty()) continue;

            JLabel categoryLabel = new JLabel(category.getCategory());
            categoryLabel.setFont(categoryLabel.getFont().deriveFont(Font.BOLD, 14f));
            categoryLabel.setIcon(category.getIcon());
            categoryLabel.setBorder(BorderFactory.createEmptyBorder(8, 4, 4, 4));
            categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            cardsPanel.add(categoryLabel);

            for (JIPipeStatisticsItem item : items) {
                JPanel card = createCard(item);
                card.setAlignmentX(Component.LEFT_ALIGNMENT);
                cardsPanel.add(card);
                cardsPanel.add(Box.createVerticalStrut(4));
            }
        }

        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton sendNowButton = UIUtils.createButton("Send now", JIPipe.RESOURCES.getIcon16("actions/mail-send.png"), () -> {
            workbench.sendStatusBarText("Sending usage statistics...");
            StatisticsReporter.sendNow(success -> {
                SwingUtilities.invokeLater(() -> {
                    if (success) {
                        workbench.sendStatusBarText("Statistics sent.");
                    } else {
                        workbench.sendStatusBarText("Failed to send statistics (will retry later).");
                    }
                });
            });
        });
        buttonPanel.add(sendNowButton);

        JButton closeButton = UIUtils.createButton("Close", JIPipe.RESOURCES.getIcon16("actions/cancel.png"), () -> setVisible(false));
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createCard(JIPipeStatisticsItem item) {
        JPanel card = new JPanel(new BorderLayout(8, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIUtils.getControlBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        card.setBackground(UIManager.getColor("TextField.background"));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel iconLabel = new JLabel(item.getCategory().getIcon());
        card.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        textPanel.add(nameLabel);

        JsonNode serialized = item.serialize();
        String valueStr = StringUtils.nullToEmpty(serialized != null ? serialized.toString() : "");
        JLabel valueLabel = new JLabel(valueStr);
        valueLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        textPanel.add(valueLabel);

        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }
}
