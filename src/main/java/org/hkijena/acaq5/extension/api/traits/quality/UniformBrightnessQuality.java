package org.hkijena.acaq5.extension.api.traits.quality;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Uniform brightness image", description = "The image has a uniform brightness")
public class UniformBrightnessQuality extends BrightnessImageQuality {
    public UniformBrightnessQuality(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
