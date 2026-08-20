package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationEntryBuilderActionTest {

    @Test
    public void testBuilderWithAction() {
        JIPipeNotificationAction action = new JIPipeNotificationAction("Update", "Update tooltip", null, wb -> {});
        JIPipeValidationReportContext context = new UnspecifiedValidationReportContext();
        JIPipeValidationReportEntry entry = context.error()
                .title("Test title")
                .explanation("Test explanation")
                .action(action)
                .build();

        assertEquals(1, entry.getActions().size());
        assertEquals("Update", entry.getActions().get(0).getLabel());
    }

    @Test
    public void testBuilderWithMultipleActions() {
        JIPipeNotificationAction action1 = new JIPipeNotificationAction("Update", null, null, wb -> {});
        JIPipeNotificationAction action2 = new JIPipeNotificationAction("Fix", null, null, wb -> {});
        JIPipeValidationReportContext context = new UnspecifiedValidationReportContext();
        JIPipeValidationReportEntry entry = context.error()
                .title("Test title")
                .action(action1)
                .action(action2)
                .build();

        assertEquals(2, entry.getActions().size());
    }

    @Test
    public void testBuilderReportCarriesActions() {
        JIPipeNotificationAction action = new JIPipeNotificationAction("Update", null, null, wb -> {});
        JIPipeValidationReportContext context = new UnspecifiedValidationReportContext();
        JIPipeValidationReport report = new JIPipeValidationReport();
        JIPipeValidationReportEntry entry = context.error()
                .title("Test title")
                .action(action)
                .report(report);

        assertEquals(1, entry.getActions().size());
        assertEquals(1, report.size());
    }
}
