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

package org.hkijena.jipipe.extensions.tools;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.scijava.plugin.Plugin;

/**
 * Extension containing some additional tools
 */
@Plugin(type = JIPipeJavaExtension.class)
public class ToolsExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "Standard tools";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Provides some additional tools.");
    }

    @Override
    public void register() {
//        registerMenuExtension(ScreenshotWholeGraphToolPNG.class);
//        registerMenuExtension(ScreenshotWholeGraphToolSVG.class);
        registerMenuExtension(OpenImageJTool.class);
        registerMenuExtension(CreateLaunchersTool.class);
        registerMenuExtension(CloseAllImageJWindowsTool.class);
        registerMenuExtension(RebuildAliasIdsTool.class);
        registerMenuExtension(DissolveCompartmentsTool.class);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:settings";
    }

    @Override
    public String getDependencyVersion() {
        return "1.39.2";
    }
}
