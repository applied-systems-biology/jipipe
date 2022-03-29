package org.hkijena.jipipe.extensions.cellpose.compat;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseModelData;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@JIPipeDocumentation(name = "Import Cellpose model file", description = "Imports a Cellpose model file.")
public class CellPoseModelImageJImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(CellPoseModelData.class);
        Path modelPath = Paths.get(parameters.getName());
        CellPoseModelData cellPoseModelData = new CellPoseModelData(modelPath);
        dataTable.addData(cellPoseModelData, progressInfo);
        return dataTable;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return CellPoseModelData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return Object.class;
    }
}
