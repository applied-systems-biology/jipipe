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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationResults;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.ScreenshotSource;
import org.hkijena.jipipe.desktop.utils.ScreenshotRenderer;
import org.hkijena.jipipe.utils.ui.ScreenImage;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Built-in screenshot operations: graph canvas, minimap, UI surfaces, and the project window.
 * <p>
 * All rendering that needs live components or a visible window is delegated to the
 * {@link ScreenshotSource} seam: the operations run headless-side, so the desktop wiring
 * registers the production source via {@link ScreenshotOperations#setSource(ScreenshotSource)}
 * when a project window is available (same seam pattern as the cache trigger). Without a
 * registered source the operations return an error result instead of failing.
 * <p>
 * Every result goes through {@link AutomationResults#choose(BufferedImage, String)}: with a
 * save-to target only the written file path is returned, never the inline image (size
 * contract).
 */
public class ScreenshotOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Description clause shared by all four screenshot tools: where the optional save-to target
     * may point (the same allowed roots as export_data).
     */
    private static final String SAVE_TO_DESCRIPTION =
            " The optional save-to target must point inside an allowed export root: the "
                    + "jipipe-mcp-export directory in the system temp directory, or the project "
                    + "work directory. Targets outside these roots are refused (error naming the roots); "
                    + "a blank save-to falls back to the inline image.";

    /**
     * Error carried by all operations when no source is registered: the operations are
     * registered unconditionally, but rendering needs the desktop wiring.
     */
    static final String NO_SOURCE_ERROR = "screenshot source not available - MCP UI integration not active";

    private static volatile ScreenshotSource source;

    /**
     * Registers the production screenshot source. Called by the desktop application wiring
     * when a project window is available.
     */
    public static void setSource(ScreenshotSource screenshotSource) {
        source = screenshotSource;
    }

    /**
     * Returns the registered screenshot source, or null if none is active.
     */
    public static ScreenshotSource getSource() {
        return source;
    }

    /**
     * Removes the registered source (used on shutdown and by tests).
     */
    public static void clearSource() {
        source = null;
    }

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    private static String textOrNull(JsonNode params, String field) {
        return params.has(field) && !params.get(field).isNull() ? params.get(field).asText() : null;
    }

    private static double scaleParam(JsonNode params) {
        return params.path("scale").asDouble(2.0);
    }

    /**
     * Routes a rendered screenshot through the result envelope: a non-blank save-to target
     * that resolves inside the allowed roots ({@link AutomationOperationUtils#resolveWriteTarget},
     * the same roots as export_data) is written as PNG (parent directories are created) and only
     * the path is returned; a blank save-to falls back to the inline image exactly like
     * {@link AutomationResults#choose(BufferedImage, String)}; a target outside the allowed roots
     * is refused with an error naming them. All failures (including an unparseable path) are
     * returned as the error field, never thrown.
     */
    private static JsonNode finish(AutomationContext ctx, BufferedImage image, JsonNode params) {
        String saveTo = textOrNull(params, "save-to");
        if (saveTo == null || saveTo.isBlank()) {
            return AutomationResults.choose(image, null);
        }
        try {
            AutomationOperationUtils.TargetPath resolved = AutomationOperationUtils.resolveWriteTarget(
                    ctx.getProject(), "save-to", saveTo.trim());
            if (resolved.path() == null) {
                return error(resolved.errorMessage());
            }
            Path target = resolved.path();
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            ImageIO.write(image, "png", target.toFile());
            return AutomationResults.choose(image, target.toString());
        } catch (Exception e) {
            return error("Failed to save screenshot: " + e.getMessage());
        }
    }

    /**
     * Captures a UI surface component on the event dispatch thread. Visible components are
     * captured via {@link ScreenImage#createImage(Component)}; never-shown ones are realized
     * off-screen through {@link ScreenshotRenderer#renderImage(Component, int)} (printAll
     * technique). Which path applies is decided by the component's shown state: the source
     * decides by returning a shown or a never-shown component.
     */
    private static BufferedImage captureUiSurface(Component component) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return captureUiSurfaceOnEdt(component);
        }
        final BufferedImage[] image = new BufferedImage[1];
        final Exception[] failure = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                image[0] = captureUiSurfaceOnEdt(component);
            } catch (Exception e) {
                failure[0] = e;
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
        return image[0];
    }

    private static BufferedImage captureUiSurfaceOnEdt(Component component) throws AWTException {
        if (component.isShowing()) {
            return ScreenImage.createImage(component);
        }
        return ScreenshotRenderer.renderImage(component, Math.max(component.getPreferredSize().width, 1));
    }

    /**
     * Renders the visible viewport of a compartment's graph canvas off-screen (the canvas
     * component at its current zoom and pan; same painter as the on-screen canvas, no
     * screen interaction). {@link ScreenshotMinimap} renders the full graph instead.
     */
    public static class ScreenshotCanvas implements AutomationOperation {

        private final Supplier<ScreenshotSource> sourceSupplier;

        /**
         * Production constructor: uses the globally registered screenshot source.
         */
        public ScreenshotCanvas() {
            this.sourceSupplier = ScreenshotOperations::getSource;
        }

        /**
         * Test seam: always uses the given source.
         */
        ScreenshotCanvas(ScreenshotSource source) {
            this.sourceSupplier = () -> source;
        }

        @Override
        public String getId() {
            return "screenshot_canvas";
        }

        @Override
        public String getDescription() {
            return "Render the graph canvas of a compartment as a PNG image of the current view "
                    + "(visible viewport at the current zoom/pan). Use screenshot_minimap for an image "
                    + "of the full graph." + SAVE_TO_DESCRIPTION;
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["compartment"],
                      "properties": {
                        "compartment": {"type": "string"},
                        "scale": {"type": "number", "default": 2},
                        "save-to": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Render;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ScreenshotSource screenshotSource = sourceSupplier.get();
            if (screenshotSource == null) {
                return error(NO_SOURCE_ERROR);
            }
            if (ctx.getProject() == null) {
                return error("No project loaded");
            }
            String compartment = params.get("compartment").asText();
            var resolved = AutomationOperationUtils.findCompartment(ctx.getProject(), compartment);
            if (resolved == null) {
                return error("Compartment not found: " + compartment);
            }
            BufferedImage image = screenshotSource.renderCanvas(
                    resolved.getProjectCompartmentUUID().toString(), scaleParam(params));
            if (image == null) {
                return error("No canvas available for compartment: " + compartment);
            }
            return finish(ctx, image, params);
        }
    }

    /**
     * Renders the floating-map style minimap of a compartment off-screen (through the canvas
     * paint manager's minimap painter).
     */
    public static class ScreenshotMinimap implements AutomationOperation {

        private final Supplier<ScreenshotSource> sourceSupplier;

        /**
         * Production constructor: uses the globally registered screenshot source.
         */
        public ScreenshotMinimap() {
            this.sourceSupplier = ScreenshotOperations::getSource;
        }

        /**
         * Test seam: always uses the given source.
         */
        ScreenshotMinimap(ScreenshotSource source) {
            this.sourceSupplier = () -> source;
        }

        @Override
        public String getId() {
            return "screenshot_minimap";
        }

        @Override
        public String getDescription() {
            return "Render the minimap of a compartment as a PNG image, or save it to a file." + SAVE_TO_DESCRIPTION;
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["compartment"],
                      "properties": {
                        "compartment": {"type": "string"},
                        "scale": {"type": "number", "default": 2},
                        "save-to": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Render;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ScreenshotSource screenshotSource = sourceSupplier.get();
            if (screenshotSource == null) {
                return error(NO_SOURCE_ERROR);
            }
            if (ctx.getProject() == null) {
                return error("No project loaded");
            }
            String compartment = params.get("compartment").asText();
            var resolved = AutomationOperationUtils.findCompartment(ctx.getProject(), compartment);
            if (resolved == null) {
                return error("Compartment not found: " + compartment);
            }
            BufferedImage image = screenshotSource.renderMinimap(
                    resolved.getProjectCompartmentUUID().toString(), scaleParam(params));
            if (image == null) {
                return error("No minimap available for compartment: " + compartment);
            }
            return finish(ctx, image, params);
        }
    }

    /**
     * Captures a named UI surface of the project window. The source resolves the surface
     * catalog entry; the capture technique follows the component's shown state (visible:
     * ScreenImage; never shown: off-screen realization via the printAll technique).
     */
    public static class ScreenshotUi implements AutomationOperation {

        private final Supplier<ScreenshotSource> sourceSupplier;

        /**
         * Production constructor: uses the globally registered screenshot source.
         */
        public ScreenshotUi() {
            this.sourceSupplier = ScreenshotOperations::getSource;
        }

        /**
         * Test seam: always uses the given source.
         */
        ScreenshotUi(ScreenshotSource source) {
            this.sourceSupplier = () -> source;
        }

        @Override
        public String getId() {
            return "screenshot_ui";
        }

        @Override
        public String getDescription() {
            return "Capture a named UI surface of the project window as a PNG image, or save it to a file."
                    + SAVE_TO_DESCRIPTION;
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["surface"],
                      "properties": {
                        "surface": {"type": "string"},
                        "scale": {"type": "number", "default": 2},
                        "save-to": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Render;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ScreenshotSource screenshotSource = sourceSupplier.get();
            if (screenshotSource == null) {
                return error(NO_SOURCE_ERROR);
            }
            if (ctx.getProject() == null) {
                return error("No project loaded");
            }
            String surface = params.get("surface").asText();
            Component component = screenshotSource.findUiSurface(surface);
            if (component == null) {
                return error("Unknown UI surface: " + surface);
            }
            BufferedImage captured;
            try {
                captured = captureUiSurface(component);
            } catch (Exception e) {
                return error("Screenshot failed: " + e.getMessage());
            }
            if (captured == null) {
                return error("Screenshot failed: no image captured for surface " + surface);
            }
            int scale = (int) Math.round(scaleParam(params));
            return finish(ctx, AutomationResults.scaled(captured, scale), params);
        }
    }

    /**
     * Captures the project window. The titlebar flag is passed through to the source: when
     * true, the source composes the fake titlebar (KDE 6 Breeze style, drawn with
     * {@link org.hkijena.jipipe.desktop.utils.FakeTitlebarPainter}) on top of the capture.
     */
    public static class ScreenshotWindow implements AutomationOperation {

        private final Supplier<ScreenshotSource> sourceSupplier;

        /**
         * Production constructor: uses the globally registered screenshot source.
         */
        public ScreenshotWindow() {
            this.sourceSupplier = ScreenshotOperations::getSource;
        }

        /**
         * Test seam: always uses the given source.
         */
        ScreenshotWindow(ScreenshotSource source) {
            this.sourceSupplier = () -> source;
        }

        @Override
        public String getId() {
            return "screenshot_window";
        }

        @Override
        public String getDescription() {
            return "Capture the project window as a PNG image, optionally with a fake titlebar, or save it to a file."
                    + SAVE_TO_DESCRIPTION;
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "titlebar": {"type": "boolean", "default": true},
                        "save-to": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Render;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ScreenshotSource screenshotSource = sourceSupplier.get();
            if (screenshotSource == null) {
                return error(NO_SOURCE_ERROR);
            }
            if (ctx.getProject() == null) {
                return error("No project loaded");
            }
            boolean titlebar = params.path("titlebar").asBoolean(true);
            BufferedImage image = screenshotSource.captureWindow(ctx.getProjectId(), titlebar);
            if (image == null) {
                return error("No window available for project: " + ctx.getProjectId());
            }
            return finish(ctx, image, params);
        }
    }
}
