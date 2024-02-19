package org.hkijena.jipipe.extensions.imagejalgorithms.nodes.transform;

import org.hkijena.jipipe.api.AddJIPipeDocumentationDescription;

@AddJIPipeDocumentationDescription(description = "<ul>" +
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
