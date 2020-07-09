/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.pipelinej.ui.extensions;

import com.google.common.eventbus.Subscribe;
import org.hkijena.pipelinej.ACAQDefaultRegistry;
import org.hkijena.pipelinej.api.events.ExtensionRegisteredEvent;
import org.hkijena.pipelinej.ui.components.MarkdownDocument;
import org.hkijena.pipelinej.ui.components.ReloadableValidityChecker;

/**
 * Panel that checks plugin validity
 */
public class ACAQPluginValidityCheckerPanel extends ReloadableValidityChecker {
    /**
     * Creates new instance
     */
    public ACAQPluginValidityCheckerPanel() {
        super(ACAQDefaultRegistry.getInstance(),
                MarkdownDocument.fromPluginResource("documentation/plugin-validation.md"));
        ACAQDefaultRegistry.getInstance().getEventBus().register(this);
    }

    /**
     * Triggered when a new extension is registered
     *
     * @param event Generated event
     */
    @Subscribe
    public void onExtensionRegistered(ExtensionRegisteredEvent event) {
        recheckValidity();
    }
}
