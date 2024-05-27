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

package org.hkijena.jipipe.plugins.cellpose.legacy.compat;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.plugins.cellpose.legacy.datatypes.LegacyCellposeModelData;
import org.hkijena.jipipe.plugins.cellpose.legacy.datatypes.LegacyCellposeSizeModelData;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SetJIPipeDocumentation(name = "Import Cellpose size model file", description = "Imports a Cellpose size model file.")
public class LegacyCellposeSizeModelImageJImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(LegacyCellposeModelData.class);
        Path modelPath = Paths.get(parameters.getName());
        LegacyCellposeSizeModelData cellPoseModelData = new LegacyCellposeSizeModelData(modelPath);
        dataTable.addData(cellPoseModelData, progressInfo);
        return dataTable;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return LegacyCellposeSizeModelData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return Object.class;
    }
}
