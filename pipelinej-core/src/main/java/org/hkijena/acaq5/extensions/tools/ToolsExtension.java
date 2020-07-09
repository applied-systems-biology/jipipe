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

package org.hkijena.acaq5.extensions.tools;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.scijava.plugin.Plugin;

/**
 * Extension containing some additional tools
 */
@Plugin(type = ACAQJavaExtension.class)
public class ToolsExtension extends ACAQPrepackagedDefaultJavaExtension {
    @Override
    public String getName() {
        return "Standard tools";
    }

    @Override
    public String getDescription() {
        return "Provides some additional tools.";
    }

    @Override
    public void register() {
        registerMenuExtension(ScreenshotWholeGraphToolPNG.class);
        registerMenuExtension(ScreenshotWholeGraphToolSVG.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:settings";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
