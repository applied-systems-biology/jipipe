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
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
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
     * How long the Robot-based screen capture may take before it is abandoned (treated as
     * "no capture" and answered by the print fallback). On Wayland/XWayland hosts the JDK's
     * Robot capture goes through the xdg-desktop-portal screencast, which can block
     * indefinitely and without an exception; the bound turns that hang into a degraded
     * capture instead of a frozen application.
     */
    private static final long CAPTURE_TIMEOUT_SECONDS = 15;

    private static final AtomicInteger captureThreadId = new AtomicInteger();

    /**
     * Executes the Robot-based screen captures off the event dispatch thread: a capture
     * blocked inside the native screencast portal would otherwise freeze the whole
     * application. Daemon threads. A hung native capture can pin its pool thread until the
     * portal responds; the pool bound keeps that leak bounded, and when both threads hang,
     * further captures time out after {@link #CAPTURE_TIMEOUT_SECONDS} and take the print
     * fallback: the tool degrades, the application never freezes.
     */
    private static final ExecutorService captureExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "jipipe-mcp-capture-" + captureThreadId.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Runs the callable on the event dispatch thread and returns its result: directly on
     * the EDT, otherwise via {@link SwingUtilities#invokeAndWait}. Every Swing state read
     * and every print-based capture of the screenshot operations goes through this helper.
     */
    private static <T> T onEdt(Callable<T> callable) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return callable.call();
        }
        final Object[] result = new Object[1];
        final Exception[] failure = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                result[0] = callable.call();
            } catch (Exception e) {
                failure[0] = e;
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
        //noinspection unchecked
        return (T) result[0];
    }

    /**
     * The screen region of a showing component: the exact math of
     * {@link ScreenImage#createImage(Component)} (the component bounds translated to screen
     * coordinates). Must run on the EDT (it reads the component state).
     */
    private static Rectangle screenRegionOf(Component component) {
        Point location = new Point(0, 0);
        SwingUtilities.convertPointToScreen(location, component);
        Rectangle region = component.getBounds();
        region.x = location.x;
        region.y = location.y;
        return region;
    }

    /**
     * Runs the Robot-based capture of the screen region on {@link #captureExecutor} (never
     * on the event dispatch thread) and waits at most {@link #CAPTURE_TIMEOUT_SECONDS}. Any
     * failure (no screen access, the portal blocking the native call, the timeout) is
     * treated as "no capture": the caller takes the print fallback. An interrupted wait
     * restores the interrupt flag.
     */
    private static BufferedImage robotCaptureOffEdt(Rectangle region) {
        Future<BufferedImage> future = captureExecutor.submit(() -> ScreenImage.createImage(region));
        try {
            return future.get(CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            return null;
        } catch (ExecutionException | TimeoutException e) {
            future.cancel(true);
            return null;
        }
    }

    /**
     * The EDT phase result of the UI surface capture: whether the component was showing
     * (which decides the print technique of the fallback), and its screen region when the
     * Robot capture applies and the screen location resolved.
     */
    private record SurfaceCapturePreparation(boolean showing, Rectangle region) {
    }

    /**
     * Captures a UI surface component. Visible components are captured from the screen via
     * the Robot-based {@link ScreenImage#createImage(Rectangle)}, which never runs on the
     * event dispatch thread: the shown state and the screen region are read on the EDT (the
     * same math as {@link ScreenImage#createImage(Component)}), the capture runs off the EDT
     * with a wait bounded at {@link #CAPTURE_TIMEOUT_SECONDS}, and a capture without a result
     * (no screen access, a blocked portal, the timeout) falls back to the print path on the
     * EDT. A showing component prints in place via {@link ScreenImage#createImage(JComponent)}
     * (no reparenting, no screen access, the same technique as the window capture's print
     * fallback): the renderer would reparent the live surface into a temporary frame and
     * detach it from the project window. Never-shown components are realized off-screen
     * through {@link ScreenshotRenderer#renderImage(Component, int)}, whose reparenting is
     * the designed behavior there. Which path applies is decided by the component's shown
     * state: the source decides by returning a shown or a never-shown component.
     */
    private static BufferedImage captureUiSurface(Component component) throws Exception {
        // EDT phase: read the shown state and, when visible, the screen region.
        SurfaceCapturePreparation preparation = onEdt(() -> {
            if (!component.isShowing()) {
                return new SurfaceCapturePreparation(false, null);
            }
            try {
                return new SurfaceCapturePreparation(true, screenRegionOf(component));
            } catch (Exception e) {
                // Showing but without a usable screen location (no native peer): no
                // Robot capture, straight to the in-place print fallback.
                return new SurfaceCapturePreparation(true, null);
            }
        });
        // Off the EDT: the Robot capture with a bounded wait.
        if (preparation.region() != null) {
            BufferedImage image = robotCaptureOffEdt(preparation.region());
            if (image != null) {
                return image;
            }
        }
        // Print path, on the EDT: the fallback for a failed Robot capture (in place for
        // a showing component) and the off-screen realization for never-shown components.
        return onEdt(() -> printCapture(component, preparation.showing()));
    }

    /**
     * The print path of the UI surface capture, on the EDT. A showing component prints in
     * place via {@link ScreenImage#createImage(JComponent)}: the component's current state,
     * no reparenting, no screen access. A never-shown component is realized off-screen via
     * {@link ScreenshotRenderer#renderImage(Component, int)}, whose reparenting into a
     * temporary frame is the designed behavior there (nothing live to detach). UI surfaces
     * are Swing components.
     */
    private static BufferedImage printCapture(Component component, boolean showing) {
        if (showing) {
            return ScreenImage.createImage((JComponent) component);
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
        public boolean requiresEdt() {
            return true;
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
                        "compartment": {"type": "string", "description": "Compartment to render (UUID, alias, or name)"},
                        "scale": {"type": "number", "default": 2, "description": "Render scale factor (default 2)"},
                        "save-to": {"type": "string", "description": "Optional file path to save the PNG to; must be inside an allowed export root (the jipipe-mcp-export directory in the system temp directory, or the project work directory). Blank or omitted: the image is returned inline"}
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
        public boolean requiresEdt() {
            return true;
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
                        "compartment": {"type": "string", "description": "Compartment to render (UUID, alias, or name)"},
                        "scale": {"type": "number", "default": 2, "description": "Render scale factor (default 2)"},
                        "save-to": {"type": "string", "description": "Optional file path to save the PNG to; must be inside an allowed export root (the jipipe-mcp-export directory in the system temp directory, or the project work directory). Blank or omitted: the image is returned inline"}
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
     * the Robot-based screen capture off the EDT with an in-place print fallback; never
     * shown: off-screen realization via the printAll technique).
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

        /**
         * False: the operation bridges its Swing phases itself. The surface resolution and
         * the print path run on the EDT via {@link ScreenshotOperations#onEdt}, the Robot
         * capture runs on the bounded off-EDT executor. Dispatching the whole operation on
         * the EDT would stall it for the bounded capture wait.
         */
        @Override
        public boolean requiresEdt() {
            return false;
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
                        "surface": {"type": "string", "description": "Named UI surface of the project window to capture: one of graph-canvas, graph-editor, compartments-editor, project-tabs, settings-mcp"},
                        "scale": {"type": "number", "default": 2, "description": "Upscale factor applied to the captured image (default 2)"},
                        "save-to": {"type": "string", "description": "Optional file path to save the PNG to; must be inside an allowed export root (the jipipe-mcp-export directory in the system temp directory, or the project work directory). Blank or omitted: the image is returned inline"}
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

        /**
         * False: the operation bridges its Swing phases itself. The window resolution and
         * the print path run on the EDT through the integration's {@code callOnEdt}, the
         * Robot capture runs on the bounded off-EDT executor. Dispatching the whole
         * operation on the EDT would stall it for the bounded capture wait.
         */
        @Override
        public boolean requiresEdt() {
            return false;
        }

        @Override
        public String getDescription() {
            return "Capture the project window as a PNG image, optionally with a fake titlebar, or save it to a "
                    + "file. The capture is 1:1 (the window at its current size): unlike the other screenshot "
                    + "operations there is no scale parameter by design." + SAVE_TO_DESCRIPTION;
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "titlebar": {"type": "boolean", "default": true, "description": "Whether to compose a fake titlebar on top of the capture (default true)"},
                        "save-to": {"type": "string", "description": "Optional file path to save the PNG to; must be inside an allowed export root (the jipipe-mcp-export directory in the system temp directory, or the project work directory). Blank or omitted: the image is returned inline"}
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
