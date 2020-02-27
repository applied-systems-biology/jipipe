package org.hkijena.acaq5.extension.api.traits;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Sample", description = "Discriminates by annotation 'sample'")
public class Sample implements ACAQDiscriminator {

    private ACAQTraitDeclaration declaration;
    private String value;

    public Sample(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    public Sample(ACAQTraitDeclaration declaration, String value) {
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
