package org.hkijena.jipipe.extensions.core;

import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

/**
 * The core extension
 */
@Plugin(type = JIPipeJavaExtension.class)
public class CoreExtension extends JIPipePrepackagedDefaultJavaExtension {
    @Override
    public String getName() {
        return "Core";
    }

    @Override
    public String getDescription() {
        return "Provides core data types";
    }

    @Override
    public void register() {
        registerDatatype("jipipe:data",
                JIPipeData.class,
                ResourceUtils.getPluginResource("icons/data-types/data-type.png"),
                null,
                null);
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.jipipe:core";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }
}
