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

package org.hkijena.jipipe.extensions.imagejdatatypes.resultanalysis;

import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataDisplayOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;

public class ImportImageJPathDataOperation implements JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        UIUtils.openFileInNative(((PathData) data).toPath());
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
                fileOrFolderPath = pathData.toPath();
            } catch (IOException e) {
                return null;
            }
            return fileOrFolderPath;
        }
        return null;
    }

    @Override
    public String getId() {
        return "jipipe:import-image-into-imagej";
    }

}
