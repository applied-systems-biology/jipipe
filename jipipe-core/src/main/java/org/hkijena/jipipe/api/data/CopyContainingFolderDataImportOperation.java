/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api.data;

import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Import operation that copies the containing folder path into the clipboard. Is always added to the menu.
 */
public class CopyContainingFolderDataImportOperation implements JIPipeDataImportOperation {
    @Override
    public String getName() {
        return "Copy folder path";
    }

    @Override
    public String getDescription() {
        return "Copies the path to the folder that contains the data into the clipboard";
    }

    @Override
    public int getOrder() {
        return 9999;
    }

    @Override
    public Icon getIcon() {
        return  UIUtils.getIconFromResources("actions/document-open-folder.png");
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTable.Row row, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        try {
            Desktop.getDesktop().open(Objects.requireNonNull(rowStorageFolder.toFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
