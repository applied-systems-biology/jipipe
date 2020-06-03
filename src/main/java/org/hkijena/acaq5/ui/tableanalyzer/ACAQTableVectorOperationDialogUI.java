/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.tableanalyzer;


import org.hkijena.acaq5.ACAQDefaultRegistry;

import javax.swing.*;
import java.awt.*;

/**
 * UI for {@link ACAQTableVectorOperation}
 */
public class ACAQTableVectorOperationDialogUI extends JDialog {

    private ACAQTableVectorOperation operation;
    private boolean needsOpenDialog;
    private boolean userAccepts;

    private ACAQTableVectorOperationDialogUI(ACAQTableVectorOperation operation) {
        this.operation = operation;

        ACAQTableVectorOperationUI ui = ACAQDefaultRegistry.getInstance().getTableAnalyzerUIOperationRegistry().createUIForVectorOperation(operation);
        if (ui == null) {
            needsOpenDialog = false;
        } else {
            setLayout(new BorderLayout());
            add(ui, BorderLayout.CENTER);
        }
    }

    public ACAQTableVectorOperation getOperation() {
        return operation;
    }

    public boolean isNeedsOpenDialog() {
        return needsOpenDialog;
    }

    public boolean isUserAccepts() {
        return userAccepts;
    }
}
