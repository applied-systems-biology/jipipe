package org.hkijena.acaq5.ui.extensions;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.events.ExtensionRegisteredEvent;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.ReloadableValidityChecker;

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
