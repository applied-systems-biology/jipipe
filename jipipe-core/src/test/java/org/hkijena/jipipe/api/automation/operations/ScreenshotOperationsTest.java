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
import org.hkijena.jipipe.api.automation.ScreenshotSource;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the screenshot operation plumbing without rendering: the source seam (no source
 * registered yields the documented error), the compartment key resolution, the titlebar
 * flag pass-through, and the save-to size contract (only the path is returned, the PNG is
 * written). The rendering itself is exercised by the GUI wiring and the E2E task.
 */
public class ScreenshotOperationsTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private ScreenshotSource registeredSource;

    @BeforeAll
    static void ensureJIPipe() {
        JIPipe.ensureInstance();
    }

    @BeforeEach
    void rememberSource() {
        registeredSource = ScreenshotOperations.getSource();
        ScreenshotOperations.clearSource();
    }

    @AfterEach
    void restoreSource() {
        if (registeredSource != null) {
            ScreenshotOperations.setSource(registeredSource);
        } else {
            ScreenshotOperations.clearSource();
        }
    }

    /**
     * Fake source that returns a fixed image and records the resolved arguments.
     */
    private static class FakeSource implements ScreenshotSource {

        BufferedImage canvas = new BufferedImage(10, 8, BufferedImage.TYPE_INT_ARGB);
        BufferedImage fullCanvas = new BufferedImage(10, 8, BufferedImage.TYPE_INT_ARGB);
        String svg = "<svg/>";
        String compartmentKey;
        double scale;
        Double fullCanvasScale;
        Boolean titlebar;

        @Override
        public BufferedImage renderCanvas(String compartmentKey, double scale) {
            this.compartmentKey = compartmentKey;
            this.scale = scale;
            return canvas;
        }

        @Override
        public BufferedImage renderFullCanvas(String compartmentKey, double scale) {
            this.compartmentKey = compartmentKey;
            this.fullCanvasScale = scale;
            return fullCanvas;
        }

        @Override
        public String renderFullCanvasSvg(String compartmentKey) {
            this.compartmentKey = compartmentKey;
            return svg;
        }

        @Override
        public BufferedImage renderMinimap(String compartmentKey, double scale) {
            return canvas;
        }

        @Override
        public Component findUiSurface(String surfaceId) {
            return null;
        }

        @Override
        public BufferedImage captureWindow(String projectKey, boolean titlebar) {
            this.titlebar = titlebar;
            return canvas;
        }
    }

    private static AutomationContext createContext(JIPipeProject project) {
        InstrumentationEventBus eventBus = new InstrumentationEventBus();
        InstrumentationJobManager jobManager = new InstrumentationJobManager(eventBus, "test");
        return new AutomationContext(project, "test", new JIPipeProgressInfo(), eventBus, jobManager);
    }

    private static JIPipeProject createProject() {
        JIPipeProject project = new JIPipeProject();
        project.addCompartment("Screenshot Compartment");
        return project;
    }

    @Test
    void withoutSourceReturnsDocumentedError() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        ObjectNode params = mapper.createObjectNode().put("compartment", "Screenshot Compartment");
        var result = new ScreenshotOperations.ScreenshotCanvas().execute(ctx, params);
        assertEquals(ScreenshotOperations.NO_SOURCE_ERROR, result.get("error").asText());
        assertFalse(result.has("image-base64"));
        assertFalse(result.has("saved-to"));
    }

    @Test
    void canvasWithoutSaveToReturnsInlineImage() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        FakeSource source = new FakeSource();
        ObjectNode params = mapper.createObjectNode()
                .put("compartment", "Screenshot Compartment")
                .put("scale", 1.5);
        var result = new ScreenshotOperations.ScreenshotCanvas(source).execute(ctx, params);
        assertTrue(result.has("image-base64"), "no save-to must inline the image");
        assertEquals("image/png", result.get("mime").asText());
        assertFalse(result.has("saved-to"));
        String expectedKey = project.getCompartments().keySet().iterator().next().toString();
        assertEquals(expectedKey, source.compartmentKey, "the source must receive the compartment UUID");
        assertEquals(1.5, source.scale, 1e-9, "the scale parameter must be passed through");
    }

    @Test
    void canvasWithUnknownCompartmentReturnsError() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        ObjectNode params = mapper.createObjectNode().put("compartment", "does not exist");
        var result = new ScreenshotOperations.ScreenshotCanvas(new FakeSource()).execute(ctx, params);
        assertEquals("Compartment not found: does not exist", result.get("error").asText());
    }

    @Test
    void canvasWithSaveToWritesPngAndReturnsOnlyPath() throws Exception {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        FakeSource source = new FakeSource();
        // Inside the allowed root (the jipipe-mcp-export system directory)
        Path target = Path.of(System.getProperty("java.io.tmpdir"), "jipipe-mcp-export",
                "screenshot-out-test", "canvas.png");
        ObjectNode params = mapper.createObjectNode()
                .put("compartment", "Screenshot Compartment")
                .put("save-to", target.toString());
        var result = new ScreenshotOperations.ScreenshotCanvas(source).execute(ctx, params);
        assertTrue(Files.exists(target), "the PNG must be written");
        assertArrayEquals(source.canvas.getRGB(0, 0, 10, 8, null, 0, 10),
                ImageIO.read(target.toFile()).getRGB(0, 0, 10, 8, null, 0, 10),
                "the written PNG must be the rendered image");
        byte[] bytes = Files.readAllBytes(target);
        assertEquals(0x89, bytes[0] & 0xFF, "PNG magic byte");
        assertTrue(result.has("saved-to"), "save-to results carry the path");
        assertEquals(target.toAbsolutePath().normalize().toString(), result.get("saved-to").asText());
        assertFalse(result.has("image-base64"), "save-to results never inline the image (size contract)");
        assertFalse(result.has("mime"), "save-to results never inline the image (size contract)");
        cleanup(target.getParent());
    }

    @Test
    void canvasWithSaveToOutsideAllowedRootsIsRefusedWithErrorNamingTheRoots() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        FakeSource source = new FakeSource();
        ObjectNode params = mapper.createObjectNode()
                .put("compartment", "Screenshot Compartment")
                .put("save-to", Path.of(System.getProperty("java.io.tmpdir"), "screenshot-forbidden.png").toString());
        var result = new ScreenshotOperations.ScreenshotCanvas(source).execute(ctx, params);

        assertTrue(result.has("error"), "a save-to outside the allowed roots must be refused: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("allowed export root"), "the refusal must name the allowed roots: " + error);
        assertTrue(error.contains(Path.of(System.getProperty("java.io.tmpdir"), "jipipe-mcp-export").toString()),
                "the refusal must list the system export root: " + error);
        assertFalse(result.has("saved-to"));
        assertFalse(result.has("image-base64"));
    }

    @Test
    void canvasWithBlankSaveToFallsBackToInlineImage() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        FakeSource source = new FakeSource();
        ObjectNode params = mapper.createObjectNode()
                .put("compartment", "Screenshot Compartment")
                .put("save-to", "   ");
        var result = new ScreenshotOperations.ScreenshotCanvas(source).execute(ctx, params);

        assertTrue(result.has("image-base64"), "a blank save-to must fall back to the inline image: " + result);
        assertFalse(result.has("saved-to"), "a blank save-to must not produce a path");
    }

    @Test
    void exportGraphImageDefaultsScaleToNaturalOne() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        FakeSource source = new FakeSource();
        ObjectNode params = mapper.createObjectNode().put("compartment", "Screenshot Compartment");
        var result = new ScreenshotOperations.ExportGraphImage(source).execute(ctx, params);
        assertTrue(result.has("image-base64"), "no save-to must inline the image: " + result);
        assertEquals("image/png", result.get("mime").asText());
        assertEquals(1.0, source.fullCanvasScale, 1e-9, "no scale parameter must default to natural size");
    }

    @Test
    void exportGraphImagePassesScaleThrough() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        FakeSource source = new FakeSource();
        ObjectNode params = mapper.createObjectNode()
                .put("compartment", "Screenshot Compartment")
                .put("scale", 2.5);
        var result = new ScreenshotOperations.ExportGraphImage(source).execute(ctx, params);
        assertEquals(2.5, source.fullCanvasScale, 1e-9, "the scale parameter must be passed through");
        assertTrue(result.has("image-base64"), "no save-to must inline the image: " + result);
        assertEquals("image/png", result.get("mime").asText());
    }

    @Test
    void exportGraphImageWithSaveToWritesPngAndReturnsOnlyPath() throws Exception {
        JIPipeProject project = createProject();
        // Inside the allowed root (the project work directory)
        Path workDir = Files.createTempDirectory("jipipe-export-test");
        project.setWorkDirectory(workDir);
        AutomationContext ctx = createContext(project);
        FakeSource source = new FakeSource();
        Path target = workDir.resolve("export-graph-image.png");
        ObjectNode params = mapper.createObjectNode()
                .put("compartment", "Screenshot Compartment")
                .put("save-to", target.toString());
        var result = new ScreenshotOperations.ExportGraphImage(source).execute(ctx, params);
        assertTrue(Files.exists(target), "the PNG must be written");
        assertTrue(ImageIO.read(target.toFile()) != null, "the written file must be a decodable PNG");
        assertTrue(result.has("saved-to"), "save-to results carry the path");
        assertEquals(target.toAbsolutePath().normalize().toString(), result.get("saved-to").asText());
        assertFalse(result.has("image-base64"), "save-to results never inline the image (size contract)");
        cleanup(workDir);
    }

    @Test
    void svgWithoutSaveToIsRefused() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        FakeSource source = new FakeSource();
        ObjectNode params = mapper.createObjectNode()
                .put("compartment", "Screenshot Compartment")
                .put("format", "svg");
        var result = new ScreenshotOperations.ExportGraphImage(source).execute(ctx, params);
        assertTrue(result.has("error"), "SVG without save-to must be refused: " + result);
        String error = result.get("error").asText();
        assertTrue(error.contains("svg"), "the refusal must mention the format: " + error);
        assertTrue(error.contains("save-to"), "the refusal must mention save-to: " + error);
        assertFalse(result.has("image-base64"), "SVG is never returned inline");
    }

    @Test
    void svgWithSaveToWritesElementAndReportsSizeBytes() throws Exception {
        JIPipeProject project = createProject();
        // Inside the allowed root (the project work directory)
        Path workDir = Files.createTempDirectory("jipipe-export-test");
        project.setWorkDirectory(workDir);
        AutomationContext ctx = createContext(project);
        FakeSource source = new FakeSource();
        Path target = workDir.resolve("export-graph-image.svg");
        ObjectNode params = mapper.createObjectNode()
                .put("compartment", "Screenshot Compartment")
                .put("format", "svg")
                .put("save-to", target.toString());
        var result = new ScreenshotOperations.ExportGraphImage(source).execute(ctx, params);
        assertTrue(result.has("saved-to"), "the SVG save-to result carries the path");
        byte[] bytes = Files.readAllBytes(target);
        assertEquals(source.svg, new String(bytes, java.nio.charset.StandardCharsets.UTF_8),
                "the written file must be the SVG element");
        assertEquals(bytes.length, result.get("sizeBytes").asInt(), "sizeBytes must report the file size");
        cleanup(workDir);
    }

    @Test
    void exportGraphImageWithUnknownCompartmentReturnsError() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        ObjectNode params = mapper.createObjectNode().put("compartment", "does not exist");
        var result = new ScreenshotOperations.ExportGraphImage(new FakeSource()).execute(ctx, params);
        assertEquals("Compartment not found: does not exist", result.get("error").asText());
    }

    @Test
    void exportGraphImageWithoutSourceReturnsDocumentedError() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        ObjectNode params = mapper.createObjectNode().put("compartment", "Screenshot Compartment");
        var result = new ScreenshotOperations.ExportGraphImage().execute(ctx, params);
        assertEquals(ScreenshotOperations.NO_SOURCE_ERROR, result.get("error").asText());
        assertFalse(result.has("image-base64"));
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

    @Test
    void windowPassesTitlebarFlagThroughWithDefaultTrue() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        FakeSource source = new FakeSource();
        var operation = new ScreenshotOperations.ScreenshotWindow(source);
        var withoutFlag = operation.execute(ctx, mapper.createObjectNode());
        assertEquals(Boolean.TRUE, source.titlebar, "titlebar defaults to true");
        assertTrue(withoutFlag.has("image-base64"));
        var withFlag = operation.execute(ctx, mapper.createObjectNode().put("titlebar", false));
        assertEquals(Boolean.FALSE, source.titlebar, "the titlebar flag must be passed through");
        assertTrue(withFlag.has("image-base64"));
    }

    @Test
    void uiWithUnknownSurfaceReturnsError() {
        JIPipeProject project = createProject();
        AutomationContext ctx = createContext(project);
        ObjectNode params = mapper.createObjectNode().put("surface", "graph-toolbar");
        var result = new ScreenshotOperations.ScreenshotUi(new FakeSource()).execute(ctx, params);
        assertEquals("Unknown UI surface: graph-toolbar", result.get("error").asText());
    }
}
