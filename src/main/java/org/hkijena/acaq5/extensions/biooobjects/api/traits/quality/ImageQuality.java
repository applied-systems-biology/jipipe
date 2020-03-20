package org.hkijena.acaq5.extensions.biooobjects.api.traits.quality;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQHidden
@ACAQDocumentation(name = "Image quality")
public class ImageQuality implements ACAQTrait {
    private ACAQTraitDeclaration declaration;

    public ImageQuality(ACAQTraitDeclaration declaration) {
        this.declaration = declaration;
    }

    @Override
    public ACAQTraitDeclaration getDeclaration() {
        return declaration;
    }
}
