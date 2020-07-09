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

package org.hkijena.pipelinej.extensions.tables;

import org.hkijena.pipelinej.api.ACAQDocumentation;
import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.ui.ACAQWorkbench;

import java.nio.file.Path;

/**
 * Generates numbers from 0 to row count
 */
@ACAQDocumentation(name = "Zero table column", description = "A table column that generates zeros for each row.")
public class ZeroTableColumn implements TableColumn {
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
    public void saveTo(Path storageFilePath, String name, boolean forceName) {

    }

    @Override
    public ACAQData duplicate() {
        return new ZeroTableColumn();
    }

    @Override
    public void display(String displayName, ACAQWorkbench workbench) {

    }
}
