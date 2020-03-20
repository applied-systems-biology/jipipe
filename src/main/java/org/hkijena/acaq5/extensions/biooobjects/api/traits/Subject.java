package org.hkijena.acaq5.extensions.biooobjects.api.traits;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQDiscriminator;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Subject", description = "Discriminates by annotation 'subject'")
public class Subject implements ACAQDiscriminator {

    private ACAQTraitDeclaration declaration;
    private String value;

    public Subject(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    public Subject(ACAQTraitDeclaration declaration, String value) {
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
