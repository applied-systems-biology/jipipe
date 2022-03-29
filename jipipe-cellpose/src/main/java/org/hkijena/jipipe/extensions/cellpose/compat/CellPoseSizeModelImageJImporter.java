package org.hkijena.jipipe.extensions.cellpose.compat;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseModelData;
import org.hkijena.jipipe.extensions.cellpose.datatypes.CellPoseSizeModelData;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@JIPipeDocumentation(name = "Import Cellpose size model file", description = "Imports a Cellpose size model file.")
public class CellPoseSizeModelImageJImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable dataTable = new JIPipeDataTable(CellPoseModelData.class);
        Path modelPath = Paths.get(parameters.getName());
        CellPoseSizeModelData cellPoseModelData = new CellPoseSizeModelData(modelPath);
        dataTable.addData(cellPoseModelData, progressInfo);
        return dataTable;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return CellPoseSizeModelData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return Object.class;
    }
}
