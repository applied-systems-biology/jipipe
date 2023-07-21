package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.utils.StringUtils;

/**
 * A validity report message
 */
public class JIPipeValidationReportEntry {
    private final JIPipeValidationReportEntryLevel level;
    private final JIPipeValidationReportContext context;
    private final String title;
    private final String explanation;
    private final String solution;
    private final String details;

    /**
     * @param level       the level of this entry
     * @param context     the object that caused the problem
     * @param title       explanation what happened
     * @param explanation explanation why it happened
     * @param solution    explanation how to solve the issue
     * @param details     optional details
     */
    public JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext context, String title, String explanation, String solution, String details) {
        this.level = level;
        this.context = context != null ? context : new UnspecifiedValidationReportContext();
        this.title = title;
        this.explanation = explanation;
        this.solution = solution;
        this.details = details;
    }

    /**
     * @param level       the level of this entry
     * @param context     the object that caused the problem
     * @param title       explanation what happened
     * @param explanation explanation why it happened
     * @param solution    explanation how to solve the issue
     */
    public JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext context, String title, String explanation, String solution) {
        this(level, context, title, explanation, solution, null);
    }

    /**
     * @param level       the level of this entry
     * @param context     the object that caused the problem
     * @param title       explanation what happened
     * @param explanation explanation why it happened
     */
    public JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel level, JIPipeValidationReportContext context, String title, String explanation) {
        this(level, context, title, explanation, null, null);
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

    public JIPipeValidationReportContext getContext() {
        return context;
    }

    public String toReport() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("# ").append(level.toString()).append(": ").append(StringUtils.orElse(title, "Unnamed")).append("\n\n");

        JIPipeValidationReportContext currentContext = context;
        stringBuilder.append("## Location\n\n");
        while (currentContext != null) {
            stringBuilder.append("* ").append(currentContext.renderName()).append(" [").append(currentContext.getClass()).append("]").append("\n");
            currentContext = currentContext.getParent();
        }
        stringBuilder.append("\n");

        if (!StringUtils.isNullOrEmpty(explanation)) {
            stringBuilder.append("## Explanation\n\n").append(explanation).append("\n\n");
        }
        if (!StringUtils.isNullOrEmpty(solution)) {
            stringBuilder.append("## Suggested solution\n\n").append(solution).append("\n\n");
        }
        if (!StringUtils.isNullOrEmpty(details)) {
            stringBuilder.append("## Details\n\n```\n").append(details).append("\n```\n\n");
        }

        return stringBuilder.append("\n\n").toString();
    }

    @Override
    public String toString() {
        return "[" + level + "] " + title + " // " + explanation + " // " + solution;
    }

    public String getDetails() {
        return details;
    }
}
