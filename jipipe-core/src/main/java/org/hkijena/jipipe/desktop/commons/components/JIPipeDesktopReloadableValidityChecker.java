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

import org.hkijena.jipipe.api.validation.JIPipeValidatable;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Panel that encapsulates a {@link JIPipeDesktopValidityReportUI} and an {@link JIPipeValidatable}.
 * Allows users to reevaluate the {@link JIPipeValidatable}
 */
public class JIPipeDesktopReloadableValidityChecker extends JIPipeDesktopWorkbenchPanel {
    private final JIPipeValidatable validatable;
    private final MarkdownText helpDocument;
    private final JIPipeValidationReport report = new JIPipeValidationReport();
    private JIPipeDesktopValidityReportUI reportUI;

    /**
     * @param workbench   the workbench
     * @param validatable the validated object
     */
    public JIPipeDesktopReloadableValidityChecker(JIPipeDesktopWorkbench workbench, JIPipeValidatable validatable) {
        this(workbench, validatable, null);
    }

    /**
     * @param workbench    the workbench
     * @param validatable  the validated object
     * @param helpDocument custom documentation. Can be null
     */
    public JIPipeDesktopReloadableValidityChecker(JIPipeDesktopWorkbench workbench, JIPipeValidatable validatable, MarkdownText helpDocument) {
        super(workbench);
        this.validatable = validatable;
        this.helpDocument = helpDocument;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        reportUI = new JIPipeDesktopValidityReportUI(getDesktopWorkbench(), true, helpDocument);
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
