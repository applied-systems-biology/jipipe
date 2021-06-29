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

package org.hkijena.jipipe.extensions.tables.display;

import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.tableeditor.TableEditor;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenResultsTableInJIPipeTabDataOperation implements JIPipeDataImportOperation, JIPipeDataDisplayOperation {
    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        data.display(displayName, workbench, source);
    }

    @Override
    public String getId() {
        return "jipipe:open-table-in-jipipe-tab";
    }

    @Override
    public String getName() {
        return "Open in JIPipe (new tab)";
    }

    @Override
    public String getDescription() {
        return "Opens the table in a new tab inside JIPipe";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/jipipe.png");
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeExportedDataTableRow row, String dataAnnotationName, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench) {
        if (rowStorageFolder == null || !Files.isDirectory(rowStorageFolder))
            return null;
        Path csvFile = PathUtils.findFileByExtensionIn(rowStorageFolder, ".csv");
        if (csvFile != null) {
            ResultsTableData result = TableEditor.importTableFromCSV(csvFile, (JIPipeProjectWorkbench) workbench);
            workbench.getDocumentTabPane().switchToLastTab();
            return result;
        }
        return null;
    }
}
