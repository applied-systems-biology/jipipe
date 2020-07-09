/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.components;

import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * UI for an {@link JIPipeValidityReport}
 */
public class JIPipeValidityReportUI extends JPanel {
    private JSplitPane splitPane;
    private JIPipeValidityReport report;
    private UserFriendlyErrorUI errorUI;
    private boolean withHelp;
    private JPanel everythingValidPanel;
    private MarkdownDocument helpDocument;

    /**
     * @param withHelp if a help panel should be shown
     */
    public JIPipeValidityReportUI(boolean withHelp) {
        this(withHelp, MarkdownDocument.fromPluginResource("documentation/validation.md"));
    }

    /**
     * @param withHelp     if a help panel should be shown
     * @param helpDocument a custom help document
     */
    public JIPipeValidityReportUI(boolean withHelp, MarkdownDocument helpDocument) {
        if (helpDocument == null)
            helpDocument = MarkdownDocument.fromPluginResource("documentation/validation.md");

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
        errorUI = new UserFriendlyErrorUI(null, UserFriendlyErrorUI.WITH_SCROLLING);

        // Create alternative panel
        everythingValidPanel = new JPanel(new BorderLayout());
        {
            JLabel label = new JLabel("No issues found", UIUtils.getIconFromResources("check-circle-green-64.png"), JLabel.LEFT);
            label.setFont(label.getFont().deriveFont(26.0f));
            everythingValidPanel.add(label, BorderLayout.CENTER);
        }

        // Create split pane if needed
        if (withHelp) {
            splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JLabel(),
                    new MarkdownReader(false, helpDocument));
            splitPane.setDividerSize(3);
            splitPane.setResizeWeight(0.66);
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    splitPane.setDividerLocation(0.66);
                }
            });
            add(splitPane, BorderLayout.CENTER);
        }
    }

    private void refreshUI() {
        if (report == null || report.isValid()) {
            switchToEverythingValid();
        } else {
            errorUI.clear();
            errorUI.displayErrors(report);
            errorUI.addVerticalGlue();
            switchToErrors();
        }
    }

    public void setReport(JIPipeValidityReport report) {
        this.report = report;
        refreshUI();
    }
}
