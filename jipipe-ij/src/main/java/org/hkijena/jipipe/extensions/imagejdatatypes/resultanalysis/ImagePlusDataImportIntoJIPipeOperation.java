package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.api.data.JIPipeResultSlotDataSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.ImageViewerPanel;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

public class ImagePlusDataImportIntoJIPipeOperation implements JIPipeDataImportOperation {
    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        ImagePlusData data = ImagePlusData.importFrom(rowStorageFolder);
        ImageViewerPanel.showImage(data.getImage(), displayName);
        return data;
    }

    @Override
    public String getName() {
        return "Import into JIPipe";
    }

    @Override
    public String getDescription() {
        return "Imports the image into the JIPipe image viewer";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/jipipe.png");
    }
}
