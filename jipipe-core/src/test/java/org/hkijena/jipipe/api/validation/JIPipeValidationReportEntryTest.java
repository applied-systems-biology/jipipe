package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JIPipeValidationReportEntryTest {

    @Test
    public void testEntryWithActions() {
        JIPipeNotificationAction action1 = new JIPipeNotificationAction("Update", "Update tooltip", null, wb -> {});
        JIPipeNotificationAction action2 = new JIPipeNotificationAction("Fix", "Fix tooltip", null, wb -> {});
        JIPipeValidationReportEntry entry = new JIPipeValidationReportEntry(
                JIPipeValidationReportEntryLevel.Error,
                new UnspecifiedValidationReportContext(),
                "Title",
                "Explanation",
                "Solution",
                null,
                Arrays.asList(action1, action2));

        List<JIPipeNotificationAction> actions = entry.getActions();
        assertEquals(2, actions.size());
        assertEquals("Update", actions.get(0).getLabel());
        assertEquals("Fix", actions.get(1).getLabel());
    }

    @Test
    public void testEntryWithoutActionsDefaultsToEmptyList() {
        JIPipeValidationReportEntry entry = new JIPipeValidationReportEntry(
                JIPipeValidationReportEntryLevel.Error,
                new UnspecifiedValidationReportContext(),
                "Title",
                "Explanation",
                "Solution");

        assertTrue(entry.getActions().isEmpty());
    }

    @Test
    public void testEntryWithDetailsButNoActionsDefaultsToEmptyList() {
        JIPipeValidationReportEntry entry = new JIPipeValidationReportEntry(
                JIPipeValidationReportEntryLevel.Error,
                new UnspecifiedValidationReportContext(),
                "Title",
                "Explanation",
                "Solution",
                "details");

        assertTrue(entry.getActions().isEmpty());
    }
}
