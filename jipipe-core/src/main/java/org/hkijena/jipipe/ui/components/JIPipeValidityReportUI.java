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

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchPanel;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.markdown.MarkdownReader;
import org.hkijena.jipipe.utils.AutoResizeSplitPane;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

/**
 * UI for an {@link JIPipeValidationReport}
 */
public class JIPipeValidityReportUI extends JIPipeWorkbenchPanel {
    private final boolean withHelp;
    private final MarkdownDocument helpDocument;
    private JSplitPane splitPane;
    private JIPipeValidationReport report;
    private UserFriendlyErrorUI errorUI;
    private JPanel everythingValidPanel;

    /**
     * @param workbench the workbench
     * @param withHelp  if a help panel should be shown
     */
    public JIPipeValidityReportUI(JIPipeWorkbench workbench, boolean withHelp) {
        this(workbench, withHelp, MarkdownDocument.fromPluginResource("documentation/validation.md", new HashMap<>()));
    }

    /**
     * @param workbench    the workbench
     * @param withHelp     if a help panel should be shown
     * @param helpDocument a custom help document
     */
    public JIPipeValidityReportUI(JIPipeWorkbench workbench, boolean withHelp, MarkdownDocument helpDocument) {
        super(workbench);
        if (helpDocument == null)
            helpDocument = MarkdownDocument.fromPluginResource("documentation/validation.md", new HashMap<>());

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
        errorUI = new UserFriendlyErrorUI(getWorkbench(), null, UserFriendlyErrorUI.WITH_SCROLLING);

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
                    new MarkdownReader(false, helpDocument), AutoResizeSplitPane.RATIO_3_TO_1);
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
