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

package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.InstrumentationEventBus;
import org.hkijena.jipipe.api.automation.InstrumentationJobManager;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the get_project_report contract: the inline response (the Project Report UI's text
 * with the compartment heading at the fixed heading level 3, truncated always false), the
 * strict inline cap boundary (exactly the cap still inlines, one character over refuses with
 * the actual size and the save-to advice), and the rooted save-to alternative (the full text
 * is written UTF-8 and never inlined; targets outside the allowed roots are refused with an
 * error naming them).
 */
public class ProjectReportOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    private static AutomationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    private static JIPipeProject createProject() {
        JIPipeProject project = new JIPipeProject();
        project.addCompartment("Report Compartment");
        return project;
    }

    /**
     * Builds a project whose rendered report text (heading level 3) is exactly targetLength
     * characters long. The rendered length is affine in the single compartment's name length
     * (the name is appended raw by {@link JIPipeProject#getTextDescription(StringBuilder, int)}),
     * so the name is padded accordingly. The boundary test asserts the landing, so a rendering
     * change cannot make the test pass vacuously.
     */
    private static JIPipeProject projectWithReportOfLength(int targetLength) {
        JIPipeProject probe = new JIPipeProject();
        probe.addCompartment("Pad");
        StringBuilder probeText = new StringBuilder();
        probe.getTextDescription(probeText, 3);
        int nameLength = 3 + (targetLength - probeText.length());
        JIPipeProject project = new JIPipeProject();
        project.addCompartment("P" + "x".repeat(nameLength - 1));
        return project;
    }

    @Test
    void getProjectReport_inlineUnderCap() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        var result = new InfoOperations.GetProjectReport().execute(ctx, mapper.createObjectNode());
        assertFalse(result.has("error"), "a small project must not error: " + result);
        assertTrue(result.has("report"), "the report must be inlined: " + result);
        String report = result.get("report").asText();
        assertFalse(report.isEmpty(), "the report text must not be empty");
        assertTrue(report.contains("<h3>Compartment C1 \"Report Compartment\"</h3>"),
                "the report must contain the compartment heading at the fixed heading level 3: " + report);
        assertTrue(result.has("truncated"), "the inline response must carry the truncated field");
        assertFalse(result.get("truncated").asBoolean(),
                "truncated is always false: the cap refuses oversized reports, it never truncates them");
    }

    @Test
    void getProjectReport_capBoundaryInline() {
        // Exactly the cap still inlines (the cap is strictly >)
        JIPipeProject atCap = projectWithReportOfLength(InfoOperations.INLINE_CAP_CHARS);
        StringBuilder measured = new StringBuilder();
        atCap.getTextDescription(measured, 3);
        assertEquals(InfoOperations.INLINE_CAP_CHARS, measured.length(),
                "test setup: the synthetic project must render exactly the cap length");
        var inlined = InfoOperations.renderReport(atCap, null);
        assertFalse(inlined.has("error"), "a report of exactly the cap length must inline: " + inlined);
        assertEquals(InfoOperations.INLINE_CAP_CHARS, inlined.get("report").asText().length(),
                "the inlined report must be the full text");
        assertFalse(inlined.get("truncated").asBoolean());
        // One character over the cap refuses, naming the actual size and advising save-to
        JIPipeProject overCap = projectWithReportOfLength(InfoOperations.INLINE_CAP_CHARS + 1);
        var refused = InfoOperations.renderReport(overCap, null);
        assertTrue(refused.has("error"), "a report above the cap must be refused: " + refused);
        String error = refused.get("error").asText();
        assertTrue(error.contains(String.valueOf(InfoOperations.INLINE_CAP_CHARS + 1)),
                "the refusal must name the actual size: " + error);
        assertTrue(error.contains("save-to"), "the refusal must advise the save-to alternative: " + error);
        assertFalse(refused.has("report"), "a refused report must not inline the text");
        assertFalse(refused.has("truncated"));
    }

    @Test
    void getProjectReport_saveToInsideRootWritesFile() throws Exception {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        // Inside the allowed root (the jipipe-mcp-export system directory)
        Path target = Path.of(System.getProperty("java.io.tmpdir"), "jipipe-mcp-export",
                "project-report-test", "report.txt");
        ObjectNode params = mapper.createObjectNode().put("save-to", target.toString());
        var result = new InfoOperations.GetProjectReport().execute(ctx, params);
        assertTrue(Files.exists(target), "the report file must be written: " + result);
        StringBuilder expected = new StringBuilder();
        project.getTextDescription(expected, 3);
        assertEquals(expected.toString(), Files.readString(target, StandardCharsets.UTF_8),
                "the file must contain the full report text as UTF-8");
        assertTrue(result.has("savedTo"), "save-to results carry the path: " + result);
        assertEquals(target.toAbsolutePath().normalize().toString(), result.get("savedTo").asText());
        assertTrue(result.has("sizeBytes"), "save-to results carry the size: " + result);
        assertEquals(Files.size(target), result.get("sizeBytes").asLong());
        assertFalse(result.has("report"), "save-to results never inline the text (size contract)");
        assertFalse(result.has("truncated"), "save-to results never inline the text (size contract)");
        cleanup(target.getParent());
    }

    @Test
    void getProjectReport_saveToOutsideRootRefused() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        ObjectNode params = mapper.createObjectNode().put("save-to", "/tmp/../elsewhere/x.txt");
        var result = new InfoOperations.GetProjectReport().execute(ctx, params);
        assertTrue(result.has("error"), "a save-to outside the allowed roots must be refused: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("allowed export root"), "the refusal must name the allowed roots: " + error);
        assertTrue(error.contains(Path.of(System.getProperty("java.io.tmpdir"), "jipipe-mcp-export").toString()),
                "the refusal must list the system export root: " + error);
        assertFalse(result.has("savedTo"));
        assertFalse(result.has("report"));
    }

    /**
     * Deletes a directory tree, ignoring failures (best-effort test cleanup).
     */
    private static void cleanup(Path path) {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var entries = Files.walk(path)) {
            entries.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (Exception ignored) {
                    // Best-effort cleanup
                }
            });
        } catch (Exception ignored) {
            // Best-effort cleanup
        }
    }
}
