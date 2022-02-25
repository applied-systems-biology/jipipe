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

package org.hkijena.jipipe.extensions.utils.display;

import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemReadDataStorage;
import org.hkijena.jipipe.extensions.filesystem.dataypes.PathData;
import org.hkijena.jipipe.extensions.utils.datatypes.JIPipeOutputData;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Path;

public class ImportJIPipeProjectDataOperation implements JIPipeDataImportOperation, JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        Path folderPath = ((PathData) data).toPath();
        JIPipeProjectWindow window = (JIPipeProjectWindow) workbench.getWindow();
        window.openProject(folderPath);
    }

    @Override
    public String getId() {
        return "jipipe:open-analysis-output-in-jipipe";
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, String dataAnnotationName, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        JIPipeOutputData data = JIPipeOutputData.importData(new JIPipeFileSystemReadDataStorage(rowStorageFolder), new JIPipeProgressInfo());
        JIPipeProjectWindow window = (JIPipeProjectWindow) workbench.getWindow();
        window.openProject(data.toPath());
        return data;
    }

    @Override
    public String getName() {
        return "Open analysis output in JIPipe";
    }

    @Override
    public String getDescription() {
        return "Opens the output in the JIPipe GUI";
    }

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("actions/albumfolder-importdir.png");
    }
}
