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

import org.hkijena.jipipe.api.events.AbstractJIPipeEvent;

/**
 * Fired whenever something about the MCP automation layer changed that the status controls
 * display: lifecycle state, the running operation, or the pending Ask queue.
 * <p>
 * The event carries no payload: listeners read the current values from
 * {@link JIPipeDesktopMCPIntegration#currentSnapshot()} and the related static accessors
 * (the source may be null after an uninstall). It is emitted from background threads (the
 * endpoint's HTTP executor, the MCP lifecycle executor); listeners must marshal to the
 * event dispatch thread themselves.
 */
public class MCPStatusEvent extends AbstractJIPipeEvent {

    public MCPStatusEvent(JIPipeDesktopMCPIntegration source) {
        super(source);
    }
}
