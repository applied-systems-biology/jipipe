package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJImportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

import java.util.List;

@SetJIPipeDocumentation(name = "ROI from ROI Manager", description = "Imports ROI from the current ROI manager")
public class RoiManagerImageJImporter implements ImageJDataImporter {
    @Override
    public JIPipeDataTable importData(List<Object> objects, ImageJImportParameters parameters, JIPipeProgressInfo progressInfo) {
        JIPipeDataTable result = new JIPipeDataTable(ROIListData.class);
        if (objects != null && !objects.isEmpty()) {
            for (Object object : objects) {
                RoiManager manager = (RoiManager) object;
                ROIListData data = new ROIListData(manager);
                if (parameters.isDuplicate())
                    data = (ROIListData) data.duplicate(new JIPipeProgressInfo());
                result.addData(data, new JIPipeProgressInfo());
            }
        } else {
            RoiManager manager = RoiManager.getRoiManager();
            ROIListData data = new ROIListData(manager);
            if (parameters.isDuplicate())
                data = (ROIListData) data.duplicate(new JIPipeProgressInfo());
            result.addData(data, new JIPipeProgressInfo());
        }
        return result;
    }

    @Override
    public Class<? extends JIPipeData> getImportedJIPipeDataType() {
        return ROIListData.class;
    }

    @Override
    public Class<?> getImportedImageJDataType() {
        return RoiManager.class;
    }
}
