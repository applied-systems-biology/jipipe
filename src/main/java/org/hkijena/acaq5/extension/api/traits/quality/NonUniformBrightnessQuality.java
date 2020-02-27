package org.hkijena.acaq5.extension.api.traits.quality;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Non-uniform brightness image", description = "The image has a non-uniform brightness")
public class NonUniformBrightnessQuality extends BrightnessImageQuality {
    public NonUniformBrightnessQuality(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
