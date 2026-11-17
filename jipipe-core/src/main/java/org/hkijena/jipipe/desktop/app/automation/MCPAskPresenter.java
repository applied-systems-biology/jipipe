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

package org.hkijena.jipipe.desktop.app.automation;

import java.util.List;

/**
 * The presentation half of the MCP permission-ask flow: how a queued ask
 * ({@link JIPipeDesktopMCPIntegration.PendingAsk}) is shown to the user, and how the
 * presented asks are viewed and cleared.
 * <p>
 * This is the seam that keeps the ask presentation pluggable. Today the status bar ask
 * balloon presents the asks (the default implementation installed by
 * {@link JIPipeDesktopMCPIntegration}); the ACP integration (#1376) will register a
 * presenter that redirects asks into the active agent's chat instead. The presentation is
 * the only replaceable half: the resolution half (the {@code PendingAsk} latch, the 300 s
 * ask bound {@code JIPipeDesktopMCPIntegration.ASK_TIMEOUT_SECONDS}, and the grant-always
 * writeback) stays inside {@link JIPipeDesktopMCPIntegration#ask(String, String, String)}
 * regardless of the installed presenter. Invariant: the broker's forward timeout
 * (MCPBrokerServer.FORWARD_TIMEOUT, 360 s) must keep exceeding that ask bound, so a late
 * answer is still delivered to the agent instead of dying on a broker transport timeout.
 * <p>
 * All methods may be called from any thread: {@link #askPosted} and {@link #askRemoved}
 * run on the endpoint's HTTP executor thread, {@link #denyAll} on the uninstalling
 * thread, {@link #askAnswered} usually on the event dispatch thread (the balloon
 * buttons), and {@link #getPendingAsks} from any reader (status snapshot, AI monitor).
 * Implementations must be thread-safe.
 */
public interface MCPAskPresenter {

    /**
     * Called when one ask enters presentation: the endpoint thread queued a new request
     * and now waits for its answer. The default implementation enqueues the ask and fires
     * {@link JIPipeDesktopMCPIntegration#STATUS_EVENTS}, which drives the ask balloon and
     * the AI monitor.
     */
    void askPosted(JIPipeDesktopMCPIntegration.PendingAsk ask);

    /**
     * Called when one ask was answered (allow or deny; the first answer wins). The ask is
     * still presented at this point: {@link #askRemoved} follows once the waiting
     * endpoint thread resumes.
     */
    void askAnswered(JIPipeDesktopMCPIntegration.PendingAsk ask);

    /**
     * Called when one ask leaves presentation (answered, timed out after the ask bound,
     * or denied by a shutdown or uninstall).
     */
    void askRemoved(JIPipeDesktopMCPIntegration.PendingAsk ask);

    /**
     * The currently presented asks, oldest first. Never null; an empty list when nothing
     * is pending.
     */
    List<JIPipeDesktopMCPIntegration.PendingAsk> getPendingAsks();

    /**
     * Denies every presented ask and clears the presentation (used on uninstall: the
     * waiting endpoint threads must never block on a dead layer).
     */
    void denyAll();
}
