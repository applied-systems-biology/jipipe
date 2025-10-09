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
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.settings.application.JIPipeGeneralUIApplicationSettings;
import org.hkijena.jipipe.utils.JIPipeDesktopSplitPane;
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

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(UIUtils.createButton("New", JIPipe.RESOURCES.getIcon16("actions/document-new.png"), this::createNewTheme));
        toolBar.add(UIUtils.createButton("Edit", JIPipe.RESOURCES.getIcon16("actions/stock_edit.png"), this::editSelectedTheme));
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createIconOnlyButton("Delete", JIPipe.RESOURCES.getIcon16("actions/edit-delete.png"), this::deleteSelectedTheme));
    }

    private void deleteSelectedTheme() {

    }

    private void editSelectedTheme() {

    }

    private void createNewTheme() {

    }
}
