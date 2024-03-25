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

package org.hkijena.jipipe.plugins.clij2.compat;

import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.plugins.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.plugins.imagejdatatypes.compat.ImagePlusDataFromImageWindowImageJImporter;
import org.hkijena.jipipe.plugins.imagejdatatypes.datatypes.ImagePlusData;

import java.util.List;

@SetJIPipeDocumentation(name = "Import IJ2 dataset from ImageJ window", description = "Imports an image window into JIPipe")
public class CLIJ2DataFromImageWindowImageJImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        ImagePlusDataFromImageWindowImageJImporter importer = new ImagePlusDataFromImageWindowImageJImporter(ImagePlusData.class);
        JIPipeDataTable dataTable = importer.importData(objects, parameters, progressInfo);
        dataTable.convert(CLIJImageData.class, new JIPipeProgressInfo());
        return dataTable;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return CLIJImageData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return ImagePlus.class;
    }
}
