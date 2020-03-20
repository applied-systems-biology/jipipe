package org.hkijena.acaq5.extensions.biooobjects.api.traits.quality;

import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQHidden;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;

@ACAQHidden
@ACAQDocumentation(name = "Image brightness")
public class BrightnessImageQuality extends ImageQuality {
    public BrightnessImageQuality(ACAQTraitDeclaration declaration) {
        super(declaration);
    }
}
