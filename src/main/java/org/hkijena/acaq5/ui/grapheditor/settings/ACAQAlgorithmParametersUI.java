package org.hkijena.acaq5.ui.grapheditor.settings;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.ui.components.ACAQParameterAccessUI;
import org.hkijena.acaq5.ui.events.ReloadSettingsRequestedEvent;

public class ACAQAlgorithmParametersUI extends ACAQParameterAccessUI {

    private ACAQAlgorithm algorithm;

    public ACAQAlgorithmParametersUI(ACAQAlgorithm algorithm, String defaultHelpDocumentPath, boolean documentationBelow, boolean withDocumentation) {
        super(algorithm, defaultHelpDocumentPath, documentationBelow, withDocumentation);
        this.algorithm = algorithm;

        algorithm.getEventBus().register(this);
    }

    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    @Subscribe
    public void onReloadRequested(ReloadSettingsRequestedEvent event) {
        reloadForm();
    }
}
