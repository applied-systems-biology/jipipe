package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.transform;

import org.hkijena.jipipe.api.JIPipeDocumentationDescription;

@JIPipeDocumentationDescription(description = "<ul>" +
        "<li>Constant: Set the border to a constant value (zero/black unless configurable)</li>" +
        "<li>Repeat: Repeat the nearest non-border pixel</li>" +
        "<li>Mirror: Mirror the pixel values across the border</li>" +
        "<li>Tile: Tile the image</li>" +
        "</ul>")
public enum BorderMode {
    Constant,
    Repeat,
    Mirror,
    Tile
}
