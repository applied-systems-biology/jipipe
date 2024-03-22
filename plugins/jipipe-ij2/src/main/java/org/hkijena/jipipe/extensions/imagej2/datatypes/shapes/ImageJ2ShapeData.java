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

package org.hkijena.jipipe.extensions.imagej2.datatypes.shapes;

import net.imglib2.algorithm.neighborhood.Shape;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.utils.JIPipeSerializedParameterCollectionData;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

@SetJIPipeDocumentation(name = "IJ2 Shape", description = "An ImageJ2 shape generator")
@JIPipeDataStorageDocumentation(humanReadableDescription = "This is a generic data type. The storage folder is empty.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
public abstract class ImageJ2ShapeData extends JIPipeSerializedParameterCollectionData {


    @Override
    public void display(String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        // TODO implement
    }

    public abstract Shape createShape();
}
