package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import ij.IJ;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeExportedDataTable;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

public class OMEImageDataImportViaBioFormatsOperation implements JIPipeDataImportOperation {
    @Override
    public String getName() {
        return "Bio formats import (Plugin)";
    }

    @Override
    public String getDescription() {
        return "Imports the file via Bio formats using the Bio-Formats plugin";
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/bioformats.png");
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        Path targetFile = PathUtils.findFileByExtensionIn(rowStorageFolder, ".ome.tif");
        IJ.run("Bio-Formats Importer", "open=[" + targetFile + "]");
        return null;
    }
}
