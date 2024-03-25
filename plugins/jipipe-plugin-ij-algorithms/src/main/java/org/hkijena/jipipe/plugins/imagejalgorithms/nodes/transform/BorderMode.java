/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.nodes.transform;

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
