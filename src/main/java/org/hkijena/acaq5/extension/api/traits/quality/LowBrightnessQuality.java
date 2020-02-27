package org.hkijena.acaq5.extension.api.traits.quality;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQDocumentation(name = "Low image brightness", description = "The image has a low brightness")
public class LowBrightnessQuality extends BrightnessImageQuality {
    public LowBrightnessQuality(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
