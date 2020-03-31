package org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api;

import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

/**
 * Extension that provides trait management capabilities
 */
@Plugin(type = ACAQJavaExtension.class)
public class ACAQTraitManagementExtension extends ACAQPrepackagedDefaultJavaExtension {

    @Override
    public String getName() {
        return "Annotation management";
    }

    @Override
    public String getDescription() {
        return "Data types required for annotation management";
    }

    @Override
    public String getDependencyId() {
        return "org.hkijena.acaq5:annotation-management";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    @Override
    public void register() {
        registerAlgorithm("acaq:new-trait-node", ACAQNewTraitNode.class);
        registerAlgorithm("acaq:existing-trait-node", ACAQExistingTraitNode.class);

        registerDatatype("acaq:trait-inheritance", ACAQTraitNodeInheritanceData.class,
                ResourceUtils.getPluginResource("icons/traits/trait-boolean.png"),
                null, null);
        registerDatatype("acaq:discriminator-inheritance", ACAQDiscriminatorNodeInheritanceData.class,
                ResourceUtils.getPluginResource("icons/traits/trait-text.png"),
                null, null);
    }
}
