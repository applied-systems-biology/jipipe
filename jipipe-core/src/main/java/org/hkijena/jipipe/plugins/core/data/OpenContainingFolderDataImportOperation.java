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

package org.hkijena.jipipe.plugins.core.data;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableMetadataRow;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Import operation that opens the containing folder. Is always added to the menu.
 */
public class OpenContainingFolderDataImportOperation implements JIPipeDataImportOperation {
    @Override
    public String getId() {
        return "jipipe:open-folder";
    }

    @Override
    public String getName() {
        return "Open folder";
    }

    @Override
    public String getDescription() {
        return "Opens the folder that contains the data";
    }

    @Override
    public int getOrder() {
        return 9999;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/document-open-folder.png");
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, String dataAnnotationName, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeDesktopWorkbench workbench, JIPipeProgressInfo progressInfo) {
        try {
            Desktop.getDesktop().open(Objects.requireNonNull(rowStorageFolder.toFile()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
