/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.desktop.app.customizer;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopFormPanel;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernThemeStyle;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.JIPipeModernThemeStyleParameter;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeFileChooserApplicationSettings;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.ThemeUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.debounce.StaticDebouncer;

import javax.swing.*;
import java.awt.*;

public class JIPipeDesktopThemeManager extends JFrame {

    private final JIPipeDesktopWorkbench workbench;
    private final ThemePreviewPanel themePreviewPanel = new ThemePreviewPanel();
    private final JList<JIPipeDesktopModernThemeStyle> styleJList = new JList<>();

    public JIPipeDesktopThemeManager(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        initialize();
        reloadList();
    }

    private void reloadList() {
        DefaultListModel<JIPipeDesktopModernThemeStyle> model = new DefaultListModel<>();
        for (String id : ThemeUtils.getAvailableStyleIds()) {
            model.addElement(ThemeUtils.getStyleFromId(id));
        }
        styleJList.setModel(model);
    }

    private void initialize() {
        setTitle("JIPipe - Theme manager");
        setIconImage(UIUtils.getJIPipeIcon128());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        JPanel settingsPanel = new JPanel(new BorderLayout(8,8));

        // Create split-pane
        setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT,
                UIUtils.wrapInIslandPanelIfNeeded(themePreviewPanel),
                UIUtils.wrapInIslandPanelIfNeeded(settingsPanel),
                new JIPipeDesktopSplitPane.DynamicSidebarRatio(350, false));

        getContentPane().add(splitPane, BorderLayout.CENTER);

        initializeSettingsPanel(settingsPanel);

        // Final preparation
        pack();
        setSize(1024, 768);
        setLocationRelativeTo(workbench.getWindow());
    }

    private void initializeSettingsPanel(JPanel settingsPanel) {
        settingsPanel.add(new JScrollPane(styleJList), BorderLayout.CENTER);
        styleJList.setCellRenderer(new JIPipeDesktopModernThemeStyleListCellRenderer());
        styleJList.addListSelectionListener(e -> {
            if(styleJList.getSelectedValue() != null) {
                themePreviewPanel.setThemeStyle(styleJList.getSelectedValue());
            }
        });

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(UIUtils.createButton("New", JIPipe.RESOURCES.getIcon16("actions/document-new.png"), this::createNewTheme));
        toolBar.add(UIUtils.createButton("Edit", JIPipe.RESOURCES.getIcon16("actions/stock_edit.png"), this::editSelectedTheme));
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createButton("Apply", JIPipe.RESOURCES.getIcon16("actions/dialog-ok.png"), this::applySelectedTheme));
        toolBar.add(UIUtils.createIconOnlyButton("Delete", JIPipe.RESOURCES.getIcon16("actions/edit-delete.png"), this::deleteSelectedTheme));

        settingsPanel.add(toolBar, BorderLayout.NORTH);
    }

    private void applySelectedTheme() {

        JIPipeDesktopModernThemeStyle style = styleJList.getSelectedValue();
        if(style == null || StringUtils.isNullOrEmpty(style.getId())) {
            return;
        }

        JIPipeGeneralUIApplicationSettings uiSettings = JIPipeGeneralUIApplicationSettings.getInstance();

        uiSettings.setTheme(JIPipeDesktopUITheme.Modern);
        uiSettings.setThemeStyle(new JIPipeModernThemeStyleParameter(style.getId()));

        JIPipe.getSettings().save();

        JOptionPane.showMessageDialog(workbench.getWindow(),
                "Please restart ImageJ/JIPipe to apply the settings",
                "Customize JIPipe",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedTheme() {
        JIPipeDesktopModernThemeStyle style = styleJList.getSelectedValue();
        if(style == null) {
            return;
        }
        if(style.isBuiltIn()) {
            JOptionPane.showMessageDialog(themePreviewPanel, "You cannot delete built-in styles", "Delete style", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean isCurrentStyle = ThemeUtils.getCurrentStyle().getId().equals(style.getId());
        if(isCurrentStyle) {
            if(JOptionPane.showConfirmDialog(this, "The selected style is currently in use. If you delete it, JIPipe will reset its style to the default.\n" +
                    "Do you want to delete it anyway?", "Delete style", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }

            JIPipeGeneralUIApplicationSettings uiSettings = JIPipeGeneralUIApplicationSettings.getInstance();
            uiSettings.setThemeStyle(new JIPipeModernThemeStyleParameter(ThemeUtils.DEFAULT_STYLE_ID));
            JIPipe.getSettings().save();

            JOptionPane.showMessageDialog(workbench.getWindow(),
                    "Please restart ImageJ/JIPipe to apply the settings",
                    "Customize JIPipe",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            if(JOptionPane.showConfirmDialog(this, "Do you want to delete the selected style?", "Delete style", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
        }

        ThemeUtils.deleteStyle(style.getId());
    }

    private void editSelectedTheme() {
        JIPipeDesktopModernThemeStyle style = styleJList.getSelectedValue();
        if(style == null) {
            return;
        }
        if(style.isBuiltIn()) {
            JOptionPane.showMessageDialog(themePreviewPanel, "You cannot edit built-in styles. But you can create your own style based on a copy.", "Edit style", JOptionPane.INFORMATION_MESSAGE);
            style = new JIPipeDesktopModernThemeStyle(style);
            style.setId(null);
            style.setSavePath(null);
        }

        openEditor(style);
    }

    private void createNewTheme() {
        JIPipeDesktopModernThemeStyle style = styleJList.getSelectedValue();
        if(style == null) {
            style = new JIPipeDesktopModernThemeStyle();
        }
        style = new JIPipeDesktopModernThemeStyle(style);
        style.setId(null);
        style.setSavePath(null);

        openEditor(style);
    }

    private void openEditor(JIPipeDesktopModernThemeStyle style) {
        JIPipeDesktopThemeEditorDocument document = new JIPipeDesktopThemeEditorDocument(style);
        JIPipeDesktopThemeEditor editor = new JIPipeDesktopThemeEditor(workbench, document);
        editor.setVisible(true);
    }
}
