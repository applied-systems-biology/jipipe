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

package org.hkijena.jipipe.extensions.tables.datatypes;

import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataSource;
import org.hkijena.jipipe.api.data.JIPipeDataStorageDocumentation;
import org.hkijena.jipipe.ui.JIPipeWorkbench;

import java.nio.file.Path;

/**
 * Generates numbers from 0 to row count
 */
@JIPipeDocumentation(name = "Zero table column", description = "A table column that generates zeros for each row.")
@JIPipeDataStorageDocumentation(humanReadableDescription = "This is a structural data type. The storage folder is empty.",
jsonSchemaURL = "https://jipipe.org/schemas/datatypes/jipipe-empty-data.schema.json")
public class ZeroTableColumn implements TableColumn {
    public static ZeroTableColumn importFrom(Path storagePath, JIPipeProgressInfo progressInfo) {
        return new ZeroTableColumn();
    }

    @Override
    public String getLabel() {
        return "Generate: Zeros";
    }

    @Override
    public String[] getDataAsString(int rows) {
        String[] data = new String[rows];
        for (int i = 0; i < rows; i++) {
            data[i] = "0";
        }
        return data;
    }

    @Override
    public double[] getDataAsDouble(int rows) {
        return new double[rows];
    }

    @Override
    public String getRowAsString(int row) {
        return "0";
    }

    @Override
    public double getRowAsDouble(int row) {
        return 0;
    }

    @Override
    public int getRows() {
        return 0;
    }

    @Override
    public boolean isNumeric() {
        return true;
    }

    @Override
    public boolean isUserRemovable() {
        return false;
    }

    @Override
    public void saveTo(Path storageFilePath, String name, boolean forceName, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public JIPipeData duplicate(JIPipeProgressInfo progressInfo) {
        return new ZeroTableColumn();
    }

    @Override
    public void display(String displayName, JIPipeWorkbench workbench, JIPipeDataSource source) {

    }
}
