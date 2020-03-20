package org.hkijena.acaq5.extensions.biooobjects.api.traits;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Treatment", description = "Discriminates by annotation 'treatment'")
public class Treatment implements ACAQDiscriminator {

    private ACAQTraitDeclaration declaration;
    private String value;

    public Treatment(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    public Treatment(ACAQTraitDeclaration declaration, String value) {
        this.declaration = declaration;
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public ACAQTraitDeclaration getDeclaration() {
        return declaration;
    }
}
