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

package org.hkijena.jipipe.extensions.clij2.compat;

import ij.ImagePlus;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.clij2.datatypes.CLIJImageData;
import org.hkijena.jipipe.extensions.imagejdatatypes.compat.ImagePlusDataToImageWindowImageJExporter;

import java.util.List;

@SetJIPipeDocumentation(name = "Open IJ2 Dataset in ImageJ", description = "Displays the image(s) as ImageJ windows")
public class CLIIJ2DataToImageWindowImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        ImagePlusDataToImageWindowImageJExporter exporter = new ImagePlusDataToImageWindowImageJExporter();
        return exporter.exportData(dataTable, parameters, progressInfo);
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return CLIJImageData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return ImagePlus.class;
    }
}
