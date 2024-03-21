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

package org.hkijena.jipipe.extensions.imagej2.io.data.output;

import net.imagej.Dataset;
import org.hkijena.jipipe.extensions.imagej2.datatypes.ImageJ2DatasetData;
import org.hkijena.jipipe.extensions.imagej2.io.ImageJ2ModuleIO;
import org.scijava.plugin.Plugin;

/**
 * Handling of {@link Dataset}
 * It is assumed that these refer to images (conversion to {@link ImageJ2DatasetData})
 */
@Plugin(type = ImageJ2ModuleIO.class)
public class DatasetImageJ2ModuleOutput extends DataSlotModuleOutput<Dataset, ImageJ2DatasetData> {

    @Override
    public Dataset convertJIPipeToModuleData(ImageJ2DatasetData obj) {
        return obj.getDataset();
    }

    @Override
    public ImageJ2DatasetData convertModuleToJIPipeData(Dataset obj) {
        return new ImageJ2DatasetData(obj);
    }

    @Override
    public Class<Dataset> getModuleDataType() {
        return Dataset.class;
    }

    @Override
    public Class<ImageJ2DatasetData> getJIPipeDataType() {
        return ImageJ2DatasetData.class;
    }
}
