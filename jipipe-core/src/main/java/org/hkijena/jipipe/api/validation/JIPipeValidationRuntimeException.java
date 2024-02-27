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

package org.hkijena.jipipe.api.validation;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.InternalErrorValidationReportContext;

public class JIPipeValidationRuntimeException extends RuntimeException {
    private final JIPipeValidationReport report;

    public JIPipeValidationRuntimeException(JIPipeValidationReport report) {
        this.report = report;
    }

    public JIPipeValidationRuntimeException(JIPipeValidationReportEntry entry) {
        this.report = new JIPipeValidationReport();
        report.add(entry);
    }

    public JIPipeValidationRuntimeException(Throwable e, String title, String explanation, String solution) {
        super(e);
        this.report = new JIPipeValidationReport();
        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                new CustomValidationReportContext(e.toString()),
                title,
                explanation,
                solution,
                ExceptionUtils.getStackTrace(e)));
        if (e instanceof JIPipeValidationRuntimeException) {
            mergeReport(((JIPipeValidationRuntimeException) e).report, null);
        }
        else {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new InternalErrorValidationReportContext(),
                    e.toString(),
                    e.getMessage(),
                    null,
                    ExceptionUtils.getStackTrace(e)));
        }
    }

    public JIPipeValidationRuntimeException(JIPipeValidationReportContext context, Throwable e, String title, String explanation, String solution) {
        super(e);
        this.report = new JIPipeValidationReport();
        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, context, title, explanation, solution, ExceptionUtils.getStackTrace(e)));
        if (e instanceof JIPipeValidationRuntimeException) {
            mergeReport(((JIPipeValidationRuntimeException) e).report, context);
        }
        else {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    context,
                    e.toString(),
                    e.getMessage(),
                    null,
                    ExceptionUtils.getStackTrace(e)));
        }
    }

    private void mergeReport(JIPipeValidationReport otherReport, JIPipeValidationReportContext alternativeContext) {
        boolean applicableAlternative = alternativeContext != null && !alternativeContext.traverseNavigable().isEmpty();
        for (JIPipeValidationReportEntry entry : otherReport) {
            if (applicableAlternative && entry.getContext().traverseNavigable().isEmpty()) {
                report.add(new JIPipeValidationReportEntry(entry.getLevel(),
                        alternativeContext,
                        entry.getTitle(),
                        entry.getExplanation(),
                        entry.getSolution(),
                        entry.getDetails()));
            } else {
                report.add(entry);
            }
        }
    }

    public JIPipeValidationReport getReport() {
        return report;
    }
}
