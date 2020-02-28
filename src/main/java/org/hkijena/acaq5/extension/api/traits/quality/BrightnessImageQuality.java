package org.hkijena.acaq5.extension.api.traits.quality;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.ACAQHidden;

@ACAQHidden
@ACAQDocumentation(name = "Image brightness")
public class BrightnessImageQuality extends ImageQuality {
    public BrightnessImageQuality(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
