/*
 * Copyright by Ruman Gerst
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Insitute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * This code is licensed under BSD 2-Clause
 * See the LICENSE file provided with this code for the full license.
 */

package org.hkijena.acaq5.ui.tableanalyzer;

import javax.swing.*;

/**
 * UI for {@link ACAQTableToTableOperation}
 */
public class ACAQTableToTableOperationUI extends JPanel {
    private ACAQTableToTableOperation operation;

    /**
     * @param operation the operation
     */
    public ACAQTableToTableOperationUI(ACAQTableToTableOperation operation) {
        this.operation = operation;
    }

    public ACAQTableToTableOperation getOperation() {
        return operation;
    }
}
