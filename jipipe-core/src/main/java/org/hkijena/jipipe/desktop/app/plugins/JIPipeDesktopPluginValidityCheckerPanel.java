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

package org.hkijena.jipipe.desktop.app.plugins;

import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeService;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.commons.components.JIPipeDesktopReloadableValidityChecker;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;

import java.util.HashMap;

/**
 * Panel that checks plugin validity
 */
public class JIPipeDesktopPluginValidityCheckerPanel extends JIPipeDesktopReloadableValidityChecker implements JIPipeService.ExtensionRegisteredEventListener {
    /**
     * Creates new instance
     */
    public JIPipeDesktopPluginValidityCheckerPanel(JIPipeDesktopWorkbench workbench) {
        super(workbench, JIPipe.getInstance(),
                MarkdownText.fromPluginResource("documentation/plugin-validation.md", new HashMap<>()));
        JIPipe.getInstance().getExtensionRegisteredEventEmitter().subscribeWeak(this);
    }

    @Override
    public void onJIPipeExtensionRegistered(JIPipeService.ExtensionRegisteredEvent event) {
        recheckValidity();
    }
}
