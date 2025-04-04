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
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeLegacyDataImportOperation;
import org.hkijena.jipipe.api.data.serialization.JIPipeDataTableRowInfo;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.nio.file.Path;

/**
 * Import operation that copies the containing folder path into the clipboard. Is always added to the menu.
 */
public class CopyContainingFolderDataImportOperation implements JIPipeLegacyDataImportOperation {

    @Override
    public String getId() {
        return "jipipe:copy-folder-path";
    }

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
        return 9998;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/folder-copy.png");
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeDataTableRowInfo row, String dataAnnotationName, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeDesktopWorkbench workbench, JIPipeProgressInfo progressInfo) {
        StringSelection selection = new StringSelection(rowStorageFolder.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
        workbench.sendStatusBarText("Copied '" + rowStorageFolder + "' into the clipboard");
        return null;
    }
}
