package org.hkijena.jipipe.api.validation;

import org.hkijena.jipipe.api.JIPipeWorkbench;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class JIPipeValidationRuntimeExceptionActionsTest {

    @Test
    public void testExceptionFromEntryPreservesActions() {
        JIPipeNotificationAction action = new JIPipeNotificationAction("Update", null, null, wb -> {});
        JIPipeValidationReportEntry entry = new JIPipeValidationReportEntry(
                JIPipeValidationReportEntryLevel.Error,
                new UnspecifiedValidationReportContext(),
                "Title",
                "Explanation",
                "Solution",
                null,
                Collections.singletonList(action));

        JIPipeValidationRuntimeException exception = new JIPipeValidationRuntimeException(entry);
        JIPipeValidationReport report = exception.getReport();
        assertEquals(1, report.size());
        assertEquals(1, report.get(0).getActions().size());
        assertEquals("Update", report.get(0).getActions().get(0).getLabel());
    }

    @Test
    public void testMergeReportPreservesActions() {
        JIPipeNotificationAction action = new JIPipeNotificationAction("Fix", null, null, wb -> {});
        JIPipeValidationReportEntry innerEntry = new JIPipeValidationReportEntry(
                JIPipeValidationReportEntryLevel.Error,
                new UnspecifiedValidationReportContext(),
                "Inner title",
                "Inner explanation",
                "Inner solution",
                null,
                Collections.singletonList(action));
        JIPipeValidationRuntimeException innerException = new JIPipeValidationRuntimeException(innerEntry);

        TestNavigableContext context = new TestNavigableContext();
        JIPipeValidationRuntimeException outerException = new JIPipeValidationRuntimeException(
                context, innerException, "Outer title", "Outer explanation", "Outer solution");

        JIPipeValidationReport report = outerException.getReport();
        JIPipeValidationReportEntry mergedEntry = report.stream()
                .filter(e -> "Inner title".equals(e.getTitle()))
                .findFirst()
                .orElse(null);
        assertNotNull(mergedEntry, "Merged entry should be present in the report");
        assertEquals(1, mergedEntry.getActions().size());
        assertEquals("Fix", mergedEntry.getActions().get(0).getLabel());
    }

    private static class TestNavigableContext extends JIPipeValidationReportContext implements NavigableJIPipeValidationReportContext {
        @Override
        public boolean canNavigate(JIPipeWorkbench workbench) {
            return false;
        }

        @Override
        public void navigate(JIPipeWorkbench workbench) {
        }

        @Override
        public String renderName() {
            return "Test";
        }

        @Override
        public Icon renderIcon() {
            return null;
        }
    }
}
