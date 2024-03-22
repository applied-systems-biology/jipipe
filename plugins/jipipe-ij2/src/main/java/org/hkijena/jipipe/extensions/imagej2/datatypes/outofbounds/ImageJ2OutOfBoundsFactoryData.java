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

package org.hkijena.jipipe.extensions.imagej2.datatypes.outofbounds;

import net.imglib2.outofbounds.OutOfBoundsFactory;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.api.data.utils.JIPipeSerializedParameterCollectionData;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;

@SetJIPipeDocumentation(name = "IJ2 Out Of Bounds factory", description = "An ImageJ2 generator for values outside the image bounds")
@JIPipeDataStorageDocumentation(humanReadableDescription = "This is a generic data type. The storage folder is empty.",
        jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
public abstract class ImageJ2OutOfBoundsFactoryData extends JIPipeSerializedParameterCollectionData {


    @Override
    public void display(String displayName, JIPipeDesktopWorkbench desktopWorkbench, JIPipeDataSource source) {
        // TODO implement
    }

    public abstract OutOfBoundsFactory<?, ?> createFactory();
}
