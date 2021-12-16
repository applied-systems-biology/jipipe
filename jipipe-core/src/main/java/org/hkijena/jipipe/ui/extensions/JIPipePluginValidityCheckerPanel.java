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

package org.hkijena.jipipe.ui.extensions;

import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.ui.components.markdown.MarkdownDocument;
import org.hkijena.jipipe.ui.components.ReloadableValidityChecker;

import java.util.HashMap;

/**
 * Panel that checks plugin validity
 */
public class JIPipePluginValidityCheckerPanel extends ReloadableValidityChecker {
    /**
     * Creates new instance
     */
    public JIPipePluginValidityCheckerPanel() {
        super(JIPipe.getInstance(),
                MarkdownDocument.fromPluginResource("documentation/plugin-validation.md", new HashMap<>()));
        JIPipe.getInstance().getEventBus().register(this);
    }

    /**
     * Triggered when a new extension is registered
     *
     * @param event Generated event
     */
    @Subscribe
    public void onExtensionRegistered(JIPipe.ExtensionRegisteredEvent event) {
        recheckValidity();
    }
}
