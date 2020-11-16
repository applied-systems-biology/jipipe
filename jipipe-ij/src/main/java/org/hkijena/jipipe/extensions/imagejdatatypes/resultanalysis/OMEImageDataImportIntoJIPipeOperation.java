package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import ij.ImagePlus;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.api.data.JIPipeResultSlotDataSource;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.OMEImageData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.components.ImageViewerPanel;
import org.hkijena.jipipe.utils.ImageJUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

public class OMEImageDataImportIntoJIPipeOperation implements JIPipeDataImportOperation {
    @Override
    public String getName() {
        return "Bio formats import (JIPipe viewer)";
    }

    @Override
    public String getDescription() {
        return "Imports the file via Bio formats using JIPipe's default settings";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/bioformats.png");
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        OMEImageData data = OMEImageData.importFrom(rowStorageFolder);
        ImageViewerPanel.showImage(data.getImage(), displayName);
        return data;
    }
}
