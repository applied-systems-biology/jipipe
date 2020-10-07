package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import ij.IJ;
import ij.io.Opener;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.JsonUtils;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ImportImageJPathDataOperation implements JIPipeDataImportOperation, JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench) {
        UIUtils.openFileInNative(((PathData) data).getPath());
    }

    @Override
    public String getName() {
        return "Import into ImageJ";
    }

    @Override
    public String getDescription() {
        return "Opens the path as if opened from the file browser";
    }

    @Override
    public int getOrder() {
        return 1000;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/imagej.png");
    }

    private Path getTargetPath(Path rowStorageFolder) {
        Path listFile = PathUtils.findFileByExtensionIn(rowStorageFolder, ".json");
        if (listFile != null) {
            Path fileOrFolderPath;
            try {
                PathData pathData = JsonUtils.getObjectMapper().readerFor(PathData.class).readValue(listFile.toFile());
                fileOrFolderPath = pathData.getPath();
            } catch (IOException e) {
                return null;
            }
            return fileOrFolderPath;
        }
        return null;
    }

    @Override
    public boolean canShow(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder) {
        Path targetPath = getTargetPath(rowStorageFolder);
        if (targetPath == null)
            return false;
        if (Files.isRegularFile(targetPath)) {
            String fileType = Opener.getFileFormat(targetPath.toString());
            switch (fileType) {
                case "tif":
                case "dcm":
                case "fits":
                case "pgm":
                case "jpg":
                case "gif":
                case "lut":
                case "bmp":
                case "roi": {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        Path targetPath = getTargetPath(rowStorageFolder);
        if (targetPath == null)
            return null;
        IJ.open(targetPath.toString());
        return null;
    }
}
