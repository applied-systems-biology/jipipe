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

package org.hkijena.jipipe.plugins.imagej2.datatypes.shapes;

import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.documentation.ConfigureJIPipeDataCrate;
import org.hkijena.jipipe.api.data.utils.JIPipeSerializedParameterCollectionData;

@SetJIPipeDocumentation(name = "IJ2 Shape", description = "An ImageJ2 shape generator")
@ConfigureJIPipeDataCrate(entities = {}, inherits = JIPipeSerializedParameterCollectionData.class)
public abstract class ImageJ2ShapeData extends JIPipeSerializedParameterCollectionData {


    public abstract Shape createShape();
}
