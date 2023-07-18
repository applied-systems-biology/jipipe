package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.validation.causes.UnspecifiedValidationReportContext;

/**
 * A validity report message
 */
public class JIPipeValidationReportEntry {
    private final JIPipeValidationReportEntryLevel level;
    private final JIPipeValidationReportContext cause;
    private final String title;
    private final String explanation;
    private final String solution;
    private final String details;

    /**
     * @param level    the level of this entry
     * @param cause the object that caused the problem
     * @param title explanation what happened
     * @param explanation  explanation why it happened
     * @param solution  explanation how to solve the issue
     * @param details  optional details
     */
    public JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext cause, String title, String explanation, String solution, String details) {
        this.level = level;
        this.cause = cause != null ? cause : new UnspecifiedValidationReportContext();
        this.title = title;
        this.explanation = explanation;
        this.solution = solution;
        this.details = details;
    }

    /**
     * @param level    the level of this entry
     * @param cause the object that caused the problem
     * @param title explanation what happened
     * @param explanation  explanation why it happened
     * @param solution  explanation how to solve the issue
     */
    public JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext cause, String title, String explanation, String solution) {
       this(level, cause, title, explanation, solution, null);
    }

    /**
     * @param level    the level of this entry
     * @param cause the object that caused the problem
     * @param title explanation what happened
     * @param explanation  explanation why it happened
     */
    public JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext cause, String title, String explanation) {
        this(level, cause, title, explanation, null, null);
    }

    public JIPipeValidationReportEntryLevel getLevel() {
        return level;
    }

    public String getTitle() {
        return title;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getSolution() {
        return solution;
    }

    public JIPipeValidationReportContext getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return "[" + level + "] " + title + " // " + explanation + " // " + solution;
    }

    public String getDetails() {
        return details;
    }
}
