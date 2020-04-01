package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;
import org.hkijena.acaq5.ui.registries.ACAQUITraitRegistry;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.hkijena.acaq5.utils.StringUtils;

import java.net.URL;

/**
 * Registers an {@link ACAQJsonTraitDeclaration}
 */
public class ACAQJsonTraitRegistrationTask implements ACAQTraitRegistrationTask {

    private ACAQJsonTraitDeclaration declaration;
    private ACAQDependency source;

    /**
     * @param declaration The declaration
     * @param source      The dependency that registers the declaration
     */
    public ACAQJsonTraitRegistrationTask(ACAQJsonTraitDeclaration declaration, ACAQDependency source) {
        this.declaration = declaration;
        this.source = source;
    }

    @Override
    public void register() {
        declaration.updatedInheritedDeclarations();
        ACAQTraitRegistry.getInstance().register(declaration, source);
        if (declaration.getTraitIcon() != null && !StringUtils.isNullOrEmpty(declaration.getTraitIcon().getIconName())) {
            URL resource = ResourceUtils.getPluginResource("icons/traits/" + declaration.getTraitIcon().getIconName());
            if (resource != null) {
                ACAQUITraitRegistry.getInstance().registerIcon(declaration, resource);
            }
        }
    }

    @Override
    public boolean canRegister() {
        return declaration.getInheritedIds().stream().allMatch(id -> ACAQTraitRegistry.getInstance().hasTraitWithId(id));
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        for (String id : declaration.getInheritedIds()) {
            if (!ACAQTraitRegistry.getInstance().hasTraitWithId(id)) {
                report.forCategory("Inherited Annotations").reportIsInvalid("Inherited annotation '" + id + "' is missing! Please check if required plugins are installed.");
            }
        }
    }
}
