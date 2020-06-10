package org.hkijena.acaq5.extensions.standardalgorithms;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.MergeDataSlots;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

/**
 * Provides some standard algorithms
 */
@Plugin(type = ACAQJavaExtension.class)
public class StandardAlgorithmsExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Standard algorithms";
    }

    @Override
    public String getDescription() {
        return "A set of standard algorithms to handle various ACAQ5-specific workloads";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:standard-algorithms";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {
        registerDatatype("acaq:data", ACAQData.class, ResourceUtils.getPluginResource("icons/data-types/data-type.png"), null, null);
        registerAlgorithm("enhance-merge-slots", MergeDataSlots.class);
    }
}
