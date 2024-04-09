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

package org.hkijena.jipipe.desktop.commons.components;

import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

/**
 * UI for an {@link JIPipeValidationReport}
 */
public class JIPipeDesktopValidityReportUI extends JIPipeDesktopWorkbenchPanel {
    private final boolean withHelp;
    private final MarkdownText helpDocument;
    private JSplitPane splitPane;
    private JIPipeValidationReport report;
    private JIPipeDesktopUserFriendlyErrorUI errorUI;
    private JPanel everythingValidPanel;

    /**
     * @param workbench the workbench
     * @param withHelp  if a help panel should be shown
     */
    public JIPipeDesktopValidityReportUI(JIPipeDesktopWorkbench workbench, boolean withHelp) {
        this(workbench, withHelp, MarkdownText.fromPluginResource("documentation/validation.md", new HashMap<>()));
    }

    /**
     * @param workbench    the workbench
     * @param withHelp     if a help panel should be shown
     * @param helpDocument a custom help document
     */
    public JIPipeDesktopValidityReportUI(JIPipeDesktopWorkbench workbench, boolean withHelp, MarkdownText helpDocument) {
        super(workbench);
        if (helpDocument == null)
            helpDocument = MarkdownText.fromPluginResource("documentation/validation.md", new HashMap<>());

        this.withHelp = withHelp;
        this.helpDocument = helpDocument;

        initialize();
        switchToEverythingValid();
    }

    private void switchToErrors() {
        if (withHelp) {
            splitPane.setLeftComponent(errorUI);
        } else {
            removeAll();
            add(errorUI, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    public JToolBar getErrorToolbar() {
        return errorUI.getToolBar();
    }

    private void switchToEverythingValid() {
        if (withHelp) {
            splitPane.setLeftComponent(everythingValidPanel);
        } else {
            removeAll();
            add(everythingValidPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        // Create table panel
        errorUI = new JIPipeDesktopUserFriendlyErrorUI(getDesktopWorkbench(), null, JIPipeDesktopUserFriendlyErrorUI.WITH_SCROLLING);

        // Create alternative panel
        everythingValidPanel = new JPanel(new BorderLayout());
        {
            JLabel label = new JLabel("No issues found", UIUtils.getIcon64FromResources("check-circle-green.png"), JLabel.LEFT);
            label.setFont(label.getFont().deriveFont(26.0f));
            everythingValidPanel.add(label, BorderLayout.CENTER);
        }

        // Create split pane if needed
        if (withHelp) {
            splitPane = new AutoResizeSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JLabel(),
                    new JIPipeDesktopMarkdownReader(false, helpDocument), AutoResizeSplitPane.RATIO_3_TO_1);
            add(splitPane, BorderLayout.CENTER);
        }
    }

    private void refreshUI() {
        if (report == null || report.isEmpty()) {
            switchToEverythingValid();
        } else {
            errorUI.clear();
            errorUI.displayErrors(report);
            errorUI.addVerticalGlue();
            switchToErrors();
        }
    }

    public void setReport(JIPipeValidationReport report) {
        this.report = report;
        refreshUI();
    }
}
