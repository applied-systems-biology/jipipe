package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopStatisticsConfigurationUI extends JDialog {
    private boolean configured = false;
    private StatisticsPrivacyLevel selectedLevel;

    public JIPipeDesktopStatisticsConfigurationUI(Window parent) {
        super(parent, "Statistics Configuration");
        setModal(true);
        selectedLevel = JIPipeStatisticsApplicationSettings.getInstance().getPrivacyLevel();
        initialize();
        pack();
        setSize(500, 350);
        setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));

        JPanel infoPanel = new JPanel(new BorderLayout(8, 8));
        infoPanel.add(UIUtils.createInfoLabel("Usage Statistics",
                "JIPipe collects usage statistics to secure funding for NFDI4BIOIMAGE.",
                JIPipe.RESOURCES.getIcon64("actions/chart-bar.png")), BorderLayout.NORTH);

        JComboBox<StatisticsPrivacyLevel> comboBox = new JComboBox<>(StatisticsPrivacyLevel.values());
        comboBox.setSelectedItem(selectedLevel);
        comboBox.addActionListener(e -> selectedLevel = (StatisticsPrivacyLevel) comboBox.getSelectedItem());
        infoPanel.add(comboBox, BorderLayout.CENTER);

        JLabel descLabel = new JLabel("<html>" + selectedLevel.getDescription() + "</html>");
        infoPanel.add(descLabel, BorderLayout.SOUTH);

        add(infoPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton cancelButton = UIUtils.createButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/cancel.png"), () -> {
            configured = false;
            setVisible(false);
        });
        JButton okButton = UIUtils.createButton("OK", JIPipe.RESOURCES.getIcon16("actions/checkmark.png"), () -> {
            JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
            settings.setPrivacyLevel(selectedLevel);
            JIPipe.getInstance().getApplicationSettings().saveLater();
            configured = true;
            setVisible(false);
        });
        buttons.add(cancelButton);
        buttons.add(okButton);
        add(buttons, BorderLayout.SOUTH);
    }

    public boolean isConfigured() {
        return configured;
    }
}
