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

package org.hkijena.acaq5.ui.components;

import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.BorderLayout;

/**
 * Panel that encapsulates a {@link ACAQValidityReportUI} and an {@link ACAQValidatable}.
 * Allows users to reevaluate the {@link ACAQValidatable}
 */
public class ReloadableValidityChecker extends JPanel {
    private ACAQValidatable validatable;
    private MarkdownDocument helpDocument;
    private ACAQValidityReportUI reportUI;
    private ACAQValidityReport report = new ACAQValidityReport();

    /**
     * @param validatable the validated object
     */
    public ReloadableValidityChecker(ACAQValidatable validatable) {
        this(validatable, null);
    }

    /**
     * @param validatable  the validated object
     * @param helpDocument custom documentation. Can be null
     */
    public ReloadableValidityChecker(ACAQValidatable validatable, MarkdownDocument helpDocument) {
        this.validatable = validatable;
        this.helpDocument = helpDocument;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        reportUI = new ACAQValidityReportUI(true, helpDocument);
        add(reportUI, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());

        JButton recheckButton = new JButton("Revalidate", UIUtils.getIconFromResources("checkmark.png"));
        recheckButton.addActionListener(e -> recheckValidity());
        toolBar.add(recheckButton);

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * Revalidates the object
     */
    public void recheckValidity() {
        report.clear();
        validatable.reportValidity(report);
        reportUI.setReport(report);
    }

    public ACAQValidityReport getReport() {
        return report;
    }
}
