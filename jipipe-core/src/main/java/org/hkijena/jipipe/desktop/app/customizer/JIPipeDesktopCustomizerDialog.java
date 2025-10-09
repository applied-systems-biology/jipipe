package org.hkijena.jipipe.desktop.app.customizer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class JIPipeDesktopCustomizerDialog extends JDialog implements JIPipeDesktopWorkbenchAccess {

    private final JIPipeDesktopWorkbench workbench;
    private final ThemePreviewPanel  themePreviewPanel = new ThemePreviewPanel();
    private final JIPipeDesktopFormPanel settingsPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.WITH_SCROLLING);

    private final JComboBox<JIPipeDesktopUITheme> themeJComboBox = new JComboBox<>(JIPipeDesktopUITheme.values());
    private final JComboBox<String> themeStyleJComboBox = new JComboBox<>();
    private final JSlider scaleSlider = new JSlider(25, 500, 100);
    private final JLabel scaleLabel = new JLabel("100%");
    private final JComboBox<JIPipeFileChooserApplicationSettings.FileChooserType> fileChooserTypeJComboBox = new JComboBox<>(JIPipeFileChooserApplicationSettings.FileChooserType.values());
    private final JComboBox<JIPipeFileChooserApplicationSettings.FileChooserType> fallbackFileChooserTypeJComboBox = new JComboBox<>(JIPipeFileChooserApplicationSettings.FileChooserType.values());
    private final double systemScale;

    public JIPipeDesktopCustomizerDialog(JIPipeDesktopWorkbench workbench) {
        super(workbench.getWindow());
        this.workbench = workbench;
        this.systemScale = UIUtils.getScale(workbench.getWindow());
        initialize();
        loadDefaults();
        registerEvents();
        updatePreview();
    }

    private void loadDefaults() {
        JIPipeGeneralUIApplicationSettings uiSettings = JIPipeGeneralUIApplicationSettings.getInstance();
        JIPipeFileChooserApplicationSettings fileChooserSettings = JIPipeFileChooserApplicationSettings.getInstance();
        themeStyleJComboBox.setModel(new DefaultComboBoxModel<>(ThemeUtils.getAvailableStyleIds().toArray(new String[0])));
        themeJComboBox.setSelectedItem(uiSettings.getTheme());
        themeStyleJComboBox.setSelectedItem(uiSettings.getThemeStyle().getValue());
        fileChooserTypeJComboBox.setSelectedItem(fileChooserSettings.getFileChooserType());
        fallbackFileChooserTypeJComboBox.setSelectedItem(fileChooserSettings.getSafeFallbackFileChooserType());
        int scalePercent = (int) (systemScale * 100);
        scaleSlider.setValue(getRoundedScale(scalePercent));
    }

    private void registerEvents() {
        scaleSlider.addChangeListener(e -> {
            updatePreview();
        });
        themeJComboBox.addActionListener(e -> {
            updatePreview();
        });
        themeStyleJComboBox.addActionListener(e -> {
            updatePreview();
        });
    }

    private JIPipeDesktopUITheme getCurrentlySelectedTheme() {
        if(themeJComboBox.getSelectedItem() instanceof JIPipeDesktopUITheme theme) {
            return theme;
        }
        return JIPipeDesktopUITheme.Modern;
    }

    private String getCurrentlySelectedThemeStyleId() {
        if(!StringUtils.isNullOrEmpty(themeStyleJComboBox.getSelectedItem())) {
            String id = themeStyleJComboBox.getSelectedItem().toString();
            if(ThemeUtils.getAvailableStyleIds().contains(id)) {
                return id;
            }
        }
        return "JIPipe Light";
    }

    private JIPipeDesktopModernThemeStyle getCurrentlySelectedThemeStyle() {
        return ThemeUtils.getStyleFromId(getCurrentlySelectedThemeStyleId());
    }

    private static int getRoundedScale(int scale) {
        return Math.round(scale / 25f) * 25;
    }

    private void updatePreview() {
        int roundedScale = getRoundedScale(scaleSlider.getValue());
        scaleLabel.setText(roundedScale + "%");
        themePreviewPanel.setTheme(getCurrentlySelectedTheme());
        themePreviewPanel.setThemeStyle(getCurrentlySelectedThemeStyle());

        // The scale needs to be corrected relative to the current system scale
        int scalePercent = (int) (systemScale * 100);
        themePreviewPanel.setScale(roundedScale * 1.0f / getRoundedScale(scalePercent));

    }

    private void initialize() {
        setTitle("Customize JIPipe");
        setIconImage(UIUtils.getJIPipeIcon128());
        setModal(true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(8,8));
        getContentPane().setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        // Create split-pane
        setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT,
                UIUtils.wrapInIslandPanelIfNeeded(themePreviewPanel),
                UIUtils.wrapInIslandPanelIfNeeded(settingsPanel),
                new JIPipeDesktopSplitPane.DynamicSidebarRatio(350, false));
        splitPane.setBorder(UIUtils.createEmptyBorder(8));
        splitPane.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        getContentPane().add(splitPane, BorderLayout.CENTER);
        initializeSettingsPanel();

        // Create button panel
        JPanel buttonPanel = UIUtils.makeNonOpaque(UIUtils.boxHorizontal(
                Box.createHorizontalGlue(),
                UIUtils.createButton("Cancel", JIPipe.RESOURCES.getIcon16("actions/dialog-cancel.png"), this::doActionCancel),
                UIUtils.createButton("Save", JIPipe.RESOURCES.getIcon16("actions/stock_save.png"), this::doActionSave)
        ), true);
        buttonPanel.setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        buttonPanel.setBorder(UIUtils.createEmptyBorder(8));
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Final preparation
        pack();
        setSize(1024,768);
        setLocationRelativeTo(workbench.getWindow());
    }

    private void initializeSettingsPanel() {
        settingsPanel.addGroupHeader("Theme", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        settingsPanel.addWideToForm(UIUtils.createBorderlessReadonlyTextPane("<html>Tip: You can use <strong>Tools &gt; Theme editor</strong> to create your own style.</html>", false));
        settingsPanel.addToForm(themeJComboBox, new JLabel("Theme"));
        settingsPanel.addToForm(themeStyleJComboBox, new JLabel("Style"));

        settingsPanel.addGroupHeader("UI Scale", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        settingsPanel.addWideToForm(UIUtils.createReadonlyBorderlessTextArea("If the interface is too large or too small, you can change the UI scale."));

        scaleSlider.setSnapToTicks(true);
        scaleSlider.setMajorTickSpacing(100);
        scaleSlider.setMinorTickSpacing(25);
        scaleLabel.setMinimumSize(new Dimension(50, 20));

        settingsPanel.addWideToForm(UIUtils.borderNSEWC(null, null, scaleLabel, null, scaleSlider));
        settingsPanel.addWideToForm(UIUtils.createJLabel("The preview might be slightly inaccurate", JIPipe.RESOURCES.getIcon16("emblems/emblem-information.png")));


        settingsPanel.addGroupHeader("File chooser", JIPipe.RESOURCES.getIcon16("actions/configure.png"));
        settingsPanel.addWideToForm(UIUtils.createReadonlyBorderlessTextArea("You can change the UI JIPipe uses when you open/save files."));
        settingsPanel.addToForm(fileChooserTypeJComboBox, new JLabel("Default"));
        settingsPanel.addToForm(fallbackFileChooserTypeJComboBox, new JLabel("Fallback"));
    }

    private void doActionSave() {
        JOptionPane.showMessageDialog(workbench.getWindow(),
                "Please restart ImageJ/JIPipe to apply the settings",
                "Customize JIPipe",
                JOptionPane.INFORMATION_MESSAGE);
        setVisible(false);
    }

    private void doActionCancel() {
        setVisible(false);
    }

    @Override
    public JIPipeDesktopWorkbench getWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }
}
