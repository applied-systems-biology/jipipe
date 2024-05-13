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

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchAccess;
import org.hkijena.jipipe.desktop.commons.components.markup.JIPipeDesktopMarkdownReader;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.ColorUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.RoundedLineBorder;

import javax.swing.*;
import java.awt.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays exceptions in a user-friendly way.
 * Can handle {@link JIPipeValidationRuntimeException} and {@link JIPipeValidationReport}
 */
public class JIPipeDesktopUserFriendlyErrorUI extends JIPipeDesktopFormPanel implements JIPipeDesktopWorkbenchAccess {

    private final JIPipeDesktopWorkbench workbench;
    private final JIPipeValidationReport report = new JIPipeValidationReport();
    private final JToolBar toolBar = new JToolBar();

    /**
     * @param workbench    the workbench
     * @param helpDocument the help document to be displayed. If null, no help is displayed
     * @param flags        FormPanel flags
     */
    public JIPipeDesktopUserFriendlyErrorUI(JIPipeDesktopWorkbench workbench, MarkdownText helpDocument, int flags) {
        super(helpDocument, flags);
        this.workbench = workbench;
        initialize();
    }

    private void initialize() {
        add(toolBar, BorderLayout.NORTH);
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());

        // Open in new window
        JButton openInNewWindowButton = new JButton("Open in new window", UIUtils.getIconFromResources("actions/open-in-new-window.png"));
        openInNewWindowButton.addActionListener(e -> {
            String title = "Unnamed";
            if (getDesktopWorkbench().getWindow() instanceof JFrame) {
                title = ((JFrame) getDesktopWorkbench().getWindow()).getTitle();
            }
            UIUtils.openValidityReportDialog(getDesktopWorkbench(), getDesktopWorkbench().getWindow(), report, title + " - Problems", null,
                    false);
        });
        toolBar.add(openInNewWindowButton);

        // Copy all
        JButton copyAllButton = new JButton("Copy all", UIUtils.getIconFromResources("actions/edit-copy.png"));
        copyAllButton.addActionListener(e -> {
            StringBuilder stringBuilder = new StringBuilder();
            for (JIPipeValidationReportEntry reportEntry : report) {
                stringBuilder.append(reportEntry.toReport());
            }
            UIUtils.copyToClipboard(stringBuilder.toString());
        });
        toolBar.add(copyAllButton);
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    @Override
    public void clear() {
        report.clear();
        super.clear();
    }

    /**
     * Adds an exception to the display.
     * Does not add the necessary vertical glue.
     *
     * @param e the exception
     */
    public void displayErrors(Throwable e) {
        if (e instanceof JIPipeValidationRuntimeException) {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            JIPipeValidationRuntimeException exception = (JIPipeValidationRuntimeException) e;
            for (JIPipeValidationReportEntry entry : exception.getReport()) {
                addEntry(entry);
            }
        } else {
            StringWriter writer = new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            addEntry(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new UnspecifiedValidationReportContext(),
                    "Internal error",
                    "An internal error was encountered. The message is: " + e.getMessage(),
                    null,
                    writer.toString()));
        }
        if (e.getCause() instanceof Exception) {
            displayErrors(e.getCause());
        }
    }

    /**
     * Adds a report to the display
     * Does not add the necessary vertical glue.
     *
     * @param report the report
     */
    public void displayErrors(JIPipeValidationReport report) {
        for (JIPipeValidationReportEntry issue : report) {
            addEntry(issue);
        }
    }

    /**
     * Adds an entry to the UI
     *
     * @param entry the entry
     */
    public void addEntry(JIPipeValidationReportEntry entry) {
        if (report.contains(entry)) {
            return;
        }
        report.add(entry);

        JPanel entryPanel = new JPanel(new BorderLayout());
        JIPipeDesktopFormPanel formPanel = new JIPipeDesktopFormPanel(JIPipeDesktopFormPanel.TRANSPARENT_BACKGROUND);
        entryPanel.add(formPanel, BorderLayout.CENTER);

        Color fill;
        Color border;
        Icon icon;

        switch (entry.getLevel()) {
            case Error:
                if (UIUtils.DARK_THEME) {
                    fill = new Color(0x6F000B);
                } else {
                    fill = new Color(0xFDCDD1);
                }
                icon = UIUtils.getIcon32FromResources("actions/circle-xmark.png");
                border = ColorUtils.scaleHSV(fill, 1, 0.8f, 0.8f);
                break;
            case Warning:
                if (UIUtils.DARK_THEME) {
                    fill = new Color(0x734300);
                } else {
                    fill = new Color(0xFFEBCF);
                }
                icon = UIUtils.getIcon32FromResources("actions/dialog-warning.png");
                border = ColorUtils.scaleHSV(fill, 1, 0.8f, 0.8f);
                break;
            case Info:
                if (UIUtils.DARK_THEME) {
                    fill = new Color(0x05254B);
                } else {
                    fill = new Color(0xBED0E6);
                }
                icon = UIUtils.getIcon32FromResources("actions/dialog-information.png");
                border = ColorUtils.scaleHSV(fill, 1, 0.8f, 0.8f);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        if (entry.getContext() instanceof UnspecifiedValidationReportContext) {
            entryPanel.setBackground(UIManager.getColor("Panel.background"));
        } else {
            entryPanel.setBackground(fill);
        }
        entryPanel.setBorder(new RoundedLineBorder(border, 1, 4));


        JLabel titleLabel = new JLabel(entry.getTitle(), icon, JLabel.LEFT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 4));
        titleLabel.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        titleLabel.setOpaque(false);
        formPanel.addWideToForm(titleLabel);

        if (!StringUtils.isNullOrEmpty(entry.getExplanation())) {
            JTextArea textArea = UIUtils.createReadonlyBorderlessTextArea(entry.getExplanation());
            textArea.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
            formPanel.addWideToForm(textArea);
        }

        List<JIPipeValidationReportContext> contexts = new ArrayList<>();
        NavigableJIPipeValidationReportContext navigationContext = null;
        {
            JIPipeValidationReportContext context = entry.getContext();
            do {
                if (!(context instanceof UnspecifiedValidationReportContext)) {
                    contexts.add(context);
                    if (context instanceof NavigableJIPipeValidationReportContext && ((NavigableJIPipeValidationReportContext) context).canNavigate(getDesktopWorkbench())) {
                        navigationContext = (NavigableJIPipeValidationReportContext) context;
                    }
                    context = context.getParent();
                }
            }
            while (!(context instanceof UnspecifiedValidationReportContext));
        }

        {
            JToolBar breadcrumb = new JToolBar();
            breadcrumb.setOpaque(false);
            breadcrumb.setFloatable(false);
            breadcrumb.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));
            breadcrumb.add(Box.createHorizontalStrut(8));
            formPanel.addWideToForm(breadcrumb);

            if (contexts.isEmpty()) {
                JButton button = new JButton("Internal", UIUtils.getIconFromResources("actions/system-run.png"));
                button.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                button.setOpaque(false);
                button.setBorder(null);
                breadcrumb.add(button);
            } else {
                for (int i = 0; i < contexts.size(); i++) {
                    JIPipeValidationReportContext context = contexts.get(i);
                    if (i < contexts.size() - 1) {
                        JButton button = new JButton(context.renderIcon());
                        button.setToolTipText(context.renderName());
                        button.setOpaque(false);
                        button.setBorder(null);
                        button.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
                        breadcrumb.add(button);
                        breadcrumb.add(new JLabel(UIUtils.getIconFromResources("actions/draw-triangle2.png")));
                    } else {
                        JButton button = new JButton(context.renderName(), context.renderIcon());
                        button.setToolTipText(context.renderName());
                        button.setOpaque(false);
                        button.setBorder(null);
                        breadcrumb.add(button);
                    }
                }
            }
        }

        {
            JToolBar actionBar = new JToolBar();
            actionBar.setOpaque(false);
            actionBar.setFloatable(false);
            actionBar.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            actionBar.add(Box.createHorizontalStrut(6));
            formPanel.addWideToForm(actionBar);

            if (navigationContext != null) {
                JButton navigateButton = new JButton("Go to", UIUtils.getIconFromResources("actions/go-jump.png"));
                navigateButton.setOpaque(false);
                NavigableJIPipeValidationReportContext finalNavigationContext = navigationContext;
                navigateButton.addActionListener(e -> {
                    if (finalNavigationContext.canNavigate(getDesktopWorkbench())) {
                        finalNavigationContext.navigate(getDesktopWorkbench());
                    } else {
                        JOptionPane.showMessageDialog(this, "Unable to navigate to '" + finalNavigationContext.renderName() + "'!",
                                "Navigate",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
                actionBar.add(navigateButton);
            }

            actionBar.add(Box.createHorizontalGlue());

            JButton detailsButton = new JButton("Show details", UIUtils.getIconFromResources("actions/find.png"));
            detailsButton.addActionListener(e -> {
                JIPipeDesktopMarkdownReader.showDialog(new MarkdownText(entry.toReport()), true, entry.getTitle(), this, false);
            });
            detailsButton.setOpaque(false);
            actionBar.add(detailsButton);

            JButton copyButton = new JButton("Copy", UIUtils.getIconFromResources("actions/edit-copy.png"));
            copyButton.addActionListener(e -> UIUtils.copyToClipboard(entry.toReport()));
            copyButton.setOpaque(false);
            actionBar.add(copyButton);
        }

        addWideToForm(entryPanel);
    }

    @Override
    public JIPipeDesktopWorkbench getDesktopWorkbench() {
        return workbench;
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return workbench;
    }
}
