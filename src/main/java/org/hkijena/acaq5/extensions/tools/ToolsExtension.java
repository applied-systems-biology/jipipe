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
