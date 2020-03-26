package org.hkijena.acaq5.api.registries;

import org.hkijena.acaq5.ACAQDependency;
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
}
