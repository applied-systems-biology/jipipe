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

package org.hkijena.jipipe.plugins.imagejdatatypes.algorithms.color;

import org.hkijena.jipipe.api.ConfigureJIPipeNode;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.AddJIPipeInputSlot;
import org.hkijena.jipipe.api.nodes.AddJIPipeOutputSlot;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.color.ImagePlusColorLABData;

@SetJIPipeDocumentation(name = "Convert color image to LAB colors", description = "Converts an image into an LAB image or re-interprets existing image channels as LAB. " +
        "Please note that this node is designed for color images.")
@ConfigureJIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Colors\nConvert")
@AddJIPipeInputSlot(value = ImagePlusData.class, name = "Input")
@AddJIPipeOutputSlot(value = ImagePlusColorLABData.class, name = "Output")
public class ToLABColorSpaceConverterAlgorithm extends ColorSpaceConverterAlgorithm {
    public ToLABColorSpaceConverterAlgorithm(JIPipeNodeInfo info) {
        super(info, ImagePlusColorLABData.class);
    }

    public ToLABColorSpaceConverterAlgorithm(ColorSpaceConverterAlgorithm other) {
        super(other);
    }
}
