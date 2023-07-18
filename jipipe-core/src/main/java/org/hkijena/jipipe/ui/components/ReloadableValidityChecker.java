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

import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.causes.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that encapsulates a {@link JIPipeValidityReportUI} and an {@link JIPipeValidatable}.
 * Allows users to reevaluate the {@link JIPipeValidatable}
 */
public class ReloadableValidityChecker extends JPanel {
    private final JIPipeValidatable validatable;
    private final MarkdownDocument helpDocument;
    private JIPipeValidityReportUI reportUI;
    private final JIPipeValidationReport report = new JIPipeValidationReport();

    /**
     * @param validatable the validated object
     */
    public ReloadableValidityChecker(JIPipeValidatable validatable) {
        this(validatable, null);
    }

    /**
     * @param validatable  the validated object
     * @param helpDocument custom documentation. Can be null
     */
    public ReloadableValidityChecker(JIPipeValidatable validatable, MarkdownDocument helpDocument) {
        this.validatable = validatable;
        this.helpDocument = helpDocument;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        reportUI = new JIPipeValidityReportUI(true, helpDocument);
        add(reportUI, BorderLayout.CENTER);

        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolBar.add(Box.createHorizontalGlue());

        JButton recheckButton = new JButton("Revalidate", UIUtils.getIconFromResources("actions/checkmark.png"));
        recheckButton.addActionListener(e -> recheckValidity());
        toolBar.add(recheckButton);

        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * Revalidates the object
     */
    public void recheckValidity() {
        report.clear();
        validatable.reportValidity(new UnspecifiedValidationReportContext(), report);
        reportUI.setReport(report);
    }

    public JIPipeValidationReport getReport() {
        return report;
    }
}
