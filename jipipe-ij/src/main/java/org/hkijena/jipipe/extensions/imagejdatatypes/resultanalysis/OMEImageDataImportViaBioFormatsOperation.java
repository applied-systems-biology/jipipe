package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import ij.IJ;
import org.hkijena.jipipe.api.data.*;
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
    public String getId() {
        return "jipipe:import-ome-bio-formats";
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTableRow row, String dataAnnotationName, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        Path targetFile = PathUtils.findFileByExtensionIn(rowStorageFolder, ".ome.tif");
        IJ.run("Bio-Formats Importer", "open=[" + targetFile + "]");
        return null;
    }
}
