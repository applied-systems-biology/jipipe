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
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenInNativeApplicationDataImportOperation implements JIPipeLegacyDataImportOperation {

    private final String[] extensions;
    private String name = "Open in native application";
    private String description = "Opens the file as you would open it from your file browser.";

    public OpenInNativeApplicationDataImportOperation(String name, String description, String[] extensions) {
        this.name = name;
        this.description = description;
        this.extensions = extensions;
    }

    public OpenInNativeApplicationDataImportOperation(String... extensions) {
        this.extensions = extensions;
    }

    @Override
    public String getId() {
        return "jipipe:open-in-native-application-" + String.join("-", extensions);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getOrder() {
        return 9800;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/quickopen.png");
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeDataTableRowInfo row, String dataAnnotationName, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeDesktopWorkbench workbench, JIPipeProgressInfo progressInfo) {
        if (rowStorageFolder == null || !Files.isDirectory(rowStorageFolder))
            return null;
        Path targetFile = PathUtils.findFileByExtensionIn(rowStorageFolder, extensions);
        if (targetFile != null) {
            UIUtils.openFileInNative(targetFile);
        }
        return null;
    }

}
