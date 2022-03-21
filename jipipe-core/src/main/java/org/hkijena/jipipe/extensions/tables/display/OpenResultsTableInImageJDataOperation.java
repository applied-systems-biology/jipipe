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

import ij.measure.ResultsTable;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.extensions.tables.datatypes.ResultsTableData;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class OpenResultsTableInImageJDataOperation implements JIPipeDataImportOperation, JIPipeDataDisplayOperation {

    @Override
    public String getId() {
        return "jipipe:open-table-in-imagej";
    }

    @Override
    public void display(JIPipeData data, String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {
        ((ResultsTableData) data.duplicate(new JIPipeProgressInfo())).getTable().show(displayName);
    }

    @Override
    public String getName() {
        return "Open in ImageJ";
    }

    @Override
    public String getDescription() {
        return "Opens the table in ImageJ";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public Icon getIcon() {
        return UIUtils.getIconFromResources("apps/imagej.png");
    }

    @Override
    public JIPipeData show(JIPipeDataSlot slot, JIPipeDataTableMetadataRow row, String dataAnnotationName, Path rowStorageFolder, String compartmentName, String algorithmName, String displayName, JIPipeWorkbench workbench, JIPipeProgressInfo progressInfo) {
        if (rowStorageFolder == null || !Files.isDirectory(rowStorageFolder))
            return null;
        Path csvFile = PathUtils.findFileByExtensionIn(rowStorageFolder, ".csv");
        if (csvFile != null) {
            try {
                ResultsTableData tableData = new ResultsTableData(ResultsTable.open(csvFile.toString()));
                tableData.getTable().show(displayName);
                return tableData;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
}
