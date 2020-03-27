package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ReloadableValidityChecker extends JPanel {
    private ACAQValidatable validatable;
    private ACAQValidityReportUI reportUI;
    private ACAQValidityReport report = new ACAQValidityReport();

    public ReloadableValidityChecker(ACAQValidatable validatable) {
        this.validatable = validatable;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        reportUI = new ACAQValidityReportUI(true);
        add(reportUI, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());

        JButton recheckButton = new JButton("Revalidate", UIUtils.getIconFromResources("checkmark.png"));
        recheckButton.addActionListener(e -> recheckValidity());
        toolBar.add(recheckButton);

        add(toolBar, BorderLayout.NORTH);
    }

    public void recheckValidity() {
        report.clear();
        validatable.reportValidity(report);
        reportUI.setReport(report);
    }

    public ACAQValidityReport getReport() {
        return report;
    }
}
