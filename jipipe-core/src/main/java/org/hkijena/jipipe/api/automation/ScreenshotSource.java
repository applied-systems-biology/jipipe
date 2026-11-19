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

package org.hkijena.jipipe.api.automation;

import java.awt.Component;
import java.awt.image.BufferedImage;

/**
 * Seam between the screenshot operations and the live desktop UI.
 * <p>
 * The screenshot operations run headless-side: any rendering that needs live components or
 * a visible window is delegated to this interface. The desktop application wiring registers
 * the production implementation via {@link ScreenshotOperations#setSource(ScreenshotSource)}
 * when a project window is available; tests register fakes. Without a registered source the
 * operations return an error result instead of failing.
 * <p>
 * Implementations are responsible for hopping to the event dispatch thread where needed;
 * the methods are called from the automation endpoint thread.
 */
public interface ScreenshotSource {

    /**
     * Renders the visible viewport of a compartment's graph canvas off-screen at the given
     * scale (the current view at the current zoom and pan; same painter as the on-screen
     * canvas, no screen interaction). {@link #renderMinimap} renders the full graph.
     * <p>
     * E2E ruling 2026-11-16: when the compartment has no open editor tab, the implementation
     * opens the compartment's pipeline editor on demand (without switching tabs), renders from
     * the now-live editor (the render then shows the editor's layout area at its default
     * view), and closes the editor tab afterwards if it opened it; an editor that was already
     * open stays open.
     *
     * @param compartmentKey the compartment key (UUID string as resolved by the operation)
     * @param scale          render scale (1 = native size)
     * @return the rendered canvas, or null if the compartment has no canvas in this window
     */
    BufferedImage renderCanvas(String compartmentKey, double scale);

    /**
     * Renders the floating-map style minimap of a compartment off-screen at the given scale
     * (through the canvas paint manager's minimap painter).
     * <p>
     * E2E ruling 2026-11-16: when the compartment has no open editor tab, the implementation
     * opens the compartment's pipeline editor on demand (without switching tabs), renders from
     * the now-live editor, and closes the editor tab afterwards if it opened it; an editor
     * that was already open stays open.
     *
     * @param compartmentKey the compartment key (UUID string as resolved by the operation)
     * @param scale          render scale (1 = native size)
     * @return the rendered minimap, or null if the compartment has no canvas in this window
     */
    BufferedImage renderMinimap(String compartmentKey, double scale);

    /**
     * Renders the FULL graph of a compartment's graph canvas off-screen at natural zoom (1.0):
     * full-fidelity node rendering with the same painter as the on-screen canvas, independent
     * of the user's current zoom and scroll. The implementation remembers the current zoom,
     * sets 1.0 (skipped when already 1.0), forces the layout/paint cycle for auto-opened tabs,
     * renders, and restores the old zoom in {@code finally} before closing an auto-opened
     * editor tab - all within one EDT run so nothing is painted in between.
     * <p>
     * E2E ruling 2026-11-16: same editor-on-demand contract as {@link #renderCanvas}: when the
     * compartment has no open editor tab, the implementation opens the compartment's pipeline
     * editor on demand (without switching tabs), renders from the now-live editor, and closes
     * the editor tab afterwards if it opened it; an editor that was already open stays open.
     *
     * @param compartmentKey the compartment key (UUID string as resolved by the operation)
     * @param scale          render scale (1 = native size)
     * @return the rendered canvas, or null if the compartment has no canvas in this window
     */
    BufferedImage renderFullCanvas(String compartmentKey, double scale);

    /**
     * Renders the FULL graph of a compartment's graph canvas as an SVG element string at
     * natural zoom (1.0): the SVG element (via {@code SVGGraphics2D.getSVGElement()}) of a
     * full-fidelity render with the same painter as the on-screen canvas, independent of the
     * user's current zoom and scroll. The zoom normalize/restore bracket and the
     * editor-on-demand contract are the same as in {@link #renderFullCanvas(String, double)}.
     *
     * @param compartmentKey the compartment key (UUID string as resolved by the operation)
     * @return the SVG element string, or null if the compartment has no canvas in this window
     */
    String renderFullCanvasSvg(String compartmentKey);

    /**
     * Returns the live component of a named UI surface (curated catalog implemented by the
     * desktop wiring), or null if the surface is unknown or not available. Whether the
     * returned component is already visible (captured via ScreenImage) or never shown
     * (realized off-screen via ScreenshotRenderer) is decided by the implementation: the
     * operation picks the capture technique from the component's shown state.
     *
     * @param surfaceId the surface identifier from the catalog
     * @return the live component, or null
     */
    Component findUiSurface(String surfaceId);

    /**
     * Captures the project window. Division of labor: this implementation captures the
     * window contents; when {@code titlebar} is true it composes the fake titlebar on top
     * using {@link org.hkijena.jipipe.desktop.utils.FakeTitlebarPainter} (KDE 6 Breeze
     * style) so the result is documentation-grade and theme-independent. The operation only
     * passes the flag through.
     *
     * @param projectKey the key of the project whose window is captured
     * @param titlebar   whether to compose a fake titlebar on top of the capture
     * @return the captured window, or null if no window is available for the project
     */
    BufferedImage captureWindow(String projectKey, boolean titlebar);
}
