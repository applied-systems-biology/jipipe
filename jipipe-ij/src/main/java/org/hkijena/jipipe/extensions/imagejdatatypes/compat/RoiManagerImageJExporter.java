package org.hkijena.jipipe.extensions.imagejdatatypes.compat;

import ij.plugin.frame.RoiManager;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJExportParameters;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ROIListData;

import java.util.ArrayList;
import java.util.List;

@SetJIPipeDocumentation(name = "Export to ROI Manager", description = "Exports provided ROI into the ImageJ ROI manager")
public class RoiManagerImageJExporter implements ImageJDataExporter {
    @Override
    public List<Object> exportData(JIPipeDataTable dataTable, ImageJExportParameters parameters, JIPipeProgressInfo progressInfo) {
        List<Object> result = new ArrayList<>();
        if (parameters.isActivate() && !parameters.isNoWindows()) {
            RoiManager manager = RoiManager.getRoiManager();
            if (!parameters.isAppend()) {
                manager.reset();
            }
            for (int i = 0; i < dataTable.getRowCount(); i++) {
                ROIListData data = dataTable.getData(i, ROIListData.class, new JIPipeProgressInfo());
                if (parameters.isDuplicate())
                    data = (ROIListData) data.duplicate(new JIPipeProgressInfo());
                data.addToRoiManager(manager);
            }
            result.add(manager);
        } else {
            for (int i = 0; i < dataTable.getRowCount(); i++) {
                RoiManager manager = new RoiManager();
                ROIListData data = dataTable.getData(i, ROIListData.class, new JIPipeProgressInfo());
                if (parameters.isDuplicate())
                    data = (ROIListData) data.duplicate(new JIPipeProgressInfo());
                data.addToRoiManager(manager);
                result.add(manager);
            }
        }
        return result;
    }

    @Override
    public Class<? extends JIPipeData> getExportedJIPipeDataType() {
        return ROIListData.class;
    }

    @Override
    public Class<?> getExportedImageJDataType() {
        return RoiManager.class;
    }
}
