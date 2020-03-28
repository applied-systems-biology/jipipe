package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.traits.ACAQJsonTraitDeclaration;

public class ACAQJsonTraitRegistrationTask implements ACAQTraitRegistrationTask {

    private ACAQJsonTraitDeclaration declaration;
    private ACAQDependency source;

    public ACAQJsonTraitRegistrationTask(ACAQJsonTraitDeclaration declaration, ACAQDependency source) {
        this.declaration = declaration;
        this.source = source;
    }

    @Override
    public void register() {
        declaration.updatedInheritedDeclarations();
        ACAQTraitRegistry.getInstance().register(declaration, source);
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
