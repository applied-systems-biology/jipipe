package org.hkijena.acaq5.ui.extensions;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.api.events.ExtensionRegisteredEvent;
import org.hkijena.acaq5.ui.components.MarkdownDocument;
import org.hkijena.acaq5.ui.components.ReloadableValidityChecker;

public class ACAQPluginValidityCheckerPanel extends ReloadableValidityChecker {
    public ACAQPluginValidityCheckerPanel() {
        super(ACAQDefaultRegistry.getInstance(),
                MarkdownDocument.fromPluginResource("documentation/plugin-validation.md"));
        ACAQDefaultRegistry.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onExtensionRegistered(ExtensionRegisteredEvent event) {
        recheckValidity();
    }
}
