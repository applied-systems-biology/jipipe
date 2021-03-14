package org.hkijena.jipipe.extensions.r;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.imagejdatatypes.ImageJDataTypesSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.r.algorithms.IteratingRScriptAlgorithm;
import org.hkijena.jipipe.extensions.r.parameters.RScriptParameter;
import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.plugin.Plugin;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;

@Plugin(type = JIPipeJavaExtension.class)
public class RExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "R integration";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Integrates R scripts into JIPipe");
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:r";
    }

    @Override
    public String getDependencyVersion() {
        return "2021.3";
    }

    @Override
    public List<ImageIcon> getSplashIcons() {
        return Arrays.asList(UIUtils.getIcon32FromResources("apps/rlogo_icon.png"));
    }

    @Override
    public void register() {
        registerParameterType("r-script",
                RScriptParameter.class,
                null,
                null,
                "R script",
                "An R script",
                null);
        registerSettingsSheet(RExtensionSettings.ID,
                "R integration",
                UIUtils.getIconFromResources("apps/rlogo_icon.png"),
                "Extensions",
                UIUtils.getIconFromResources("actions/plugins.png"),
                new RExtensionSettings());
        registerNodeType("r-script-iterating", IteratingRScriptAlgorithm.class, UIUtils.getIconURLFromResources("apps/rlogo_icon.png"));
    }
}
