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

public class JIPipeDesktopThemeEditor extends JFrame implements JIPipeParameterCollection.ParameterChangedEventListener {

    private final JIPipeDesktopWorkbench workbench;
    private final ThemePreviewPanel themePreviewPanel = new ThemePreviewPanel();
    private final JIPipeDesktopParameterFormPanel settingsPanel;
    private JIPipeDesktopThemeEditorDocument document = new JIPipeDesktopThemeEditorDocument();
    private final StaticDebouncer updatePreviewDebouncer;

    public JIPipeDesktopThemeEditor(JIPipeDesktopWorkbench workbench) {
        this.workbench = workbench;
        this.settingsPanel = new JIPipeDesktopParameterFormPanel(workbench, document, MarkdownText.EMPTY, JIPipeDesktopFormPanel.WITH_SCROLLING);
        this.updatePreviewDebouncer = new StaticDebouncer(250, this::refreshPreview);
        initialize();
        newDocument();
    }

    public JIPipeDesktopThemeEditor(JIPipeDesktopWorkbench workbench, JIPipeDesktopThemeEditorDocument document) {
        this.workbench = workbench;
        this.settingsPanel = new JIPipeDesktopParameterFormPanel(workbench, document, MarkdownText.EMPTY, JIPipeDesktopFormPanel.WITH_SCROLLING);
        this.updatePreviewDebouncer = new StaticDebouncer(250, this::refreshPreview);
        initialize();
        loadDocument(document);
    }

    private void initialize() {
        setTitle("JIPipe - Theme editor");
        setIconImage(UIUtils.getJIPipeIcon128());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        getContentPane().setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());

        // Create split-pane
        setBackground(ThemeUtils.getCurrentStyle().getWindowBackground());
        JIPipeDesktopSplitPane splitPane = new JIPipeDesktopSplitPane(JIPipeDesktopSplitPane.LEFT_RIGHT,
                UIUtils.wrapInIslandPanelIfNeeded(themePreviewPanel),
                UIUtils.wrapInIslandPanelIfNeeded(settingsPanel),
                new JIPipeDesktopSplitPane.DynamicSidebarRatio(350, false));

        getContentPane().add(splitPane, BorderLayout.CENTER);

        // Create toolbar
        JToolBar toolBar = new JToolBar();

        toolBar.add(UIUtils.createButton("New", JIPipe.RESOURCES.getIcon16("actions/document-new.png"), this::newDocument));
        toolBar.add(UIUtils.createButton("New from template", JIPipe.RESOURCES.getIcon16("actions/document-new-from-template.png"), this::newDocumentFromExisting));
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(UIUtils.createButton("Save", JIPipe.RESOURCES.getIcon16("actions/document-save.png"), this::saveDocument));

        toolBar.setFloatable(false);
        UIUtils.makeNonOpaque(toolBar, true);
        getContentPane().add(toolBar, BorderLayout.NORTH);

        // Final preparation
        pack();
        setSize(1024, 768);
        setLocationRelativeTo(workbench.getWindow());
    }

    private void saveDocument() {

    }

    private void newDocument() {
        String id = JIPipeGeneralUIApplicationSettings.getInstance().getThemeStyle().getValue();
        if(!ThemeUtils.getAvailableStyleIds().contains(id)) {
            id = ThemeUtils.DEFAULT_STYLE_ID;
        }
        newDocumentFromExisting(id);
    }

    private void newDocumentFromExisting() {
        Object id = JOptionPane.showInputDialog(this,
                "Please select the theme style that you want to use as base:",
                "New theme from template",
                JOptionPane.PLAIN_MESSAGE,
                null,
                ThemeUtils.getAvailableStyleIds().toArray(),
                ThemeUtils.DEFAULT_STYLE_ID);
        if(id instanceof String str) {
            newDocumentFromExisting(str);
        }
    }

    private void newDocumentFromExisting(String id) {
        newDocumentFromExisting(ThemeUtils.getStyleFromId(id));
    }

    private void newDocumentFromExisting(JIPipeDesktopModernThemeStyle style) {
        document.getParameterChangedEventEmitter().unsubscribe(this);
        document = new JIPipeDesktopThemeEditorDocument(style);
        document.getParameterChangedEventEmitter().subscribe(this);
        settingsPanel.setDisplayedParameters(document);
        setTitle("JIPipe - Theme editor - Untitled");
        refreshPreview();
    }

    private void loadDocument(JIPipeDesktopThemeEditorDocument newDocument) {
        this.document.getParameterChangedEventEmitter().unsubscribe(this);
        this.document = newDocument;
        document.getParameterChangedEventEmitter().subscribe(this);
        settingsPanel.setDisplayedParameters(document);
        if(newDocument.getSavePath() != null) {
            setTitle("JIPipe - Theme editor - " +  newDocument.getSavePath().getFileName());
        }
        else {
            setTitle("JIPipe - Theme editor - Untitled");
        }
        refreshPreview();
    }

    private void refreshPreview() {
        themePreviewPanel.setThemeStyle(document.toStyle());
    }

    @Override
    public void onParameterChanged(JIPipeParameterCollection.ParameterChangedEvent event) {
        updatePreviewDebouncer.debounce();
    }
}
