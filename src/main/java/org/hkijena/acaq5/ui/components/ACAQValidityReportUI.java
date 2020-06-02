package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * UI for an {@link ACAQValidityReport}
 */
public class ACAQValidityReportUI extends JPanel {
    private JSplitPane splitPane;
    private ACAQValidityReport report;
    private UserFriendlyErrorUI errorUI;
    private boolean withHelp;
    private JPanel everythingValidPanel;
    private MarkdownDocument helpDocument;

    /**
     * @param withHelp if a help panel should be shown
     */
    public ACAQValidityReportUI(boolean withHelp) {
        this(withHelp, MarkdownDocument.fromPluginResource("documentation/validation.md"));
    }

    /**
     * @param withHelp     if a help panel should be shown
     * @param helpDocument a custom help document
     */
    public ACAQValidityReportUI(boolean withHelp, MarkdownDocument helpDocument) {
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

    public void setReport(ACAQValidityReport report) {
        this.report = report;
        refreshUI();
    }
}
