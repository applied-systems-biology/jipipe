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

package org.hkijena.jipipe.ui.extensions;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeService;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.ReloadableValidityChecker;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;

import java.util.HashMap;

/**
 * Panel that checks plugin validity
 */
public class JIPipePluginValidityCheckerPanel extends ReloadableValidityChecker implements JIPipeService.ExtensionRegisteredEventListener {
    /**
     * Creates new instance
     */
    public JIPipePluginValidityCheckerPanel(JIPipeWorkbench workbench) {
        super(workbench, JIPipe.getInstance(),
                MarkdownDocument.fromPluginResource("documentation/plugin-validation.md", new HashMap<>()));
        JIPipe.getInstance().getExtensionRegisteredEventEmitter().subscribeWeak(this);
    }

    @Override
    public void onJIPipeExtensionRegistered(JIPipeService.ExtensionRegisteredEvent event) {
        recheckValidity();
    }
}
