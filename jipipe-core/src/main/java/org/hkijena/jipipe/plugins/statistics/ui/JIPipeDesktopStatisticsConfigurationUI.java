package org.hkijena.jipipe.plugins.statistics.ui;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.commons.theme.ui.JIPipeDesktopModernSliderUI;
import org.hkijena.jipipe.plugins.statistics.StatisticsPrivacyLevel;
import org.hkijena.jipipe.plugins.statistics.settings.JIPipeStatisticsApplicationSettings;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.util.Dictionary;
import java.util.Hashtable;

public class JIPipeDesktopStatisticsConfigurationUI extends JDialog {
    private boolean configured = false;
    private StatisticsPrivacyLevel selectedLevel;
    private JLabel descriptionLabel;

    public JIPipeDesktopStatisticsConfigurationUI(Window parent) {
        super(parent, "Statistics Configuration");
        setModal(true);
        selectedLevel = JIPipeStatisticsApplicationSettings.getInstance().getPrivacyLevel();
        initialize();
        pack();
        setSize(600, 400);
        setLocationRelativeTo(parent);
        UIUtils.addEscapeListener(this);
    }

    private void initialize() {
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(UIUtils.createInfoLabel("Usage Statistics",
                "JIPipe collects usage statistics to secure funding for NFDI4BIOIMAGE. " +
                        "You can choose what data is shared or opt out entirely.",
                JIPipe.RESOURCES.getIcon64("actions/chart-bar.png")));
        topPanel.add(Box.createVerticalStrut(16));
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel sliderLabel = new JLabel("Privacy level:");
        sliderLabel.setFont(sliderLabel.getFont().deriveFont(Font.BOLD, 13f));
        centerPanel.add(sliderLabel, BorderLayout.NORTH);

        JSlider slider = new JSlider(0, 4, selectedLevel.ordinal());
        slider.setSnapToTicks(true);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(1);
        slider.setUI(new JIPipeDesktopModernSliderUI(slider));

        Dictionary<Integer, JLabel> labels = new Hashtable<>();
        for (StatisticsPrivacyLevel level : StatisticsPrivacyLevel.values()) {
            labels.put(level.ordinal(), new JLabel(level.name()));
        }
        slider.setLabelTable(labels);

        centerPanel.add(slider, BorderLayout.CENTER);

        descriptionLabel = new JLabel("<html>" + selectedLevel.getDescription() + "</html>");
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        centerPanel.add(descriptionLabel, BorderLayout.SOUTH);

        slider.addChangeListener((ChangeEvent e) -> {
            int value = slider.getValue();
            StatisticsPrivacyLevel[] levels = StatisticsPrivacyLevel.values();
            if (value >= 0 && value < levels.length) {
                selectedLevel = levels[value];
                descriptionLabel.setText("<html><strong>" + selectedLevel.name() + "</strong>: " +
                        selectedLevel.getDescription() + "</html>");
            }
        });

        add(centerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.add(Box.createHorizontalGlue());

        JButton cancelButton = UIUtils.createButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/cancel.png"), () -> {
            configured = false;
            setVisible(false);
        });
        buttonPanel.add(cancelButton);

        JButton okButton = UIUtils.createButton("OK", JIPipe.RESOURCES.getIcon16("actions/checkmark.png"), () -> {
            JIPipeStatisticsApplicationSettings settings = JIPipeStatisticsApplicationSettings.getInstance();
            settings.setPrivacyLevel(selectedLevel);
            JIPipe.getInstance().getApplicationSettings().saveLater();
            configured = true;
            setVisible(false);
        });
        buttonPanel.add(okButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public boolean isConfigured() {
        return configured;
    }
}
