package org.hkijena.jipipe.api.validation;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.validation.causes.CustomReportEntryCause;
import org.hkijena.jipipe.api.validation.causes.UnspecifiedReportEntryCause;

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
        this.report = new JIPipeValidationReport();
        report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error, new CustomReportEntryCause(e.toString()), title, explanation, solution, ExceptionUtils.getStackTrace(e)));
    }

    public JIPipeValidationReport getReport() {
        return report;
    }
}
